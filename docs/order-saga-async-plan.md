# Kế hoạch triển khai Order Saga bất đồng bộ

Ngày tạo: 2026-11-05.

Trạng thái: Đã thống nhất kiến trúc, chưa triển khai các bước bên dưới.

## 1. Các quyết định đã chốt

- Order là orchestrator: Order ↔ Inventory, Order ↔ Payment, Order ↔ Shipping.
- Giao tiếp giữa các bước qua Kafka command/reply bất đồng bộ. Order nhận kết quả rồi quyết định bước tiếp theo.
- State machine lưu trong DB để tiếp tục xử lý sau restart; không giữ tiến trình chỉ trong bộ nhớ.
- Mỗi service vừa consume vừa publish có một bảng inbox và một bảng outbox trong DB riêng, dùng chung cho các loại message.
- Inbox chống message trùng; business key chống thao tác nghiệp vụ trùng; state/version kiểm soát chuyển bước.
- Dùng polling outbox publisher trước. Chưa cần CDC hoặc service orchestrator riêng.
- Mỗi bước triển khai phải có kiểm chứng trước khi chuyển bước tiếp theo.

## 2. Hiện trạng repo

- `order_service`: có POST `/api/orders`, POST `/api/orders/preview`, lưu order và publish `OrderPlacedEvent` trực tiếp qua KafkaTemplate.
- `PlaceOrderApplicationService`: save rồi publish trong phương thức có `@Transactional`; chưa có outbox đảm bảo liên kết DB và message.
- `PlaceOrderDomainService.crossDomainValidate()` đang trả `true`; chưa reserve kho thực.
- OrderStatus hiện có `PENDING`, `CONFIRMED`, `CANCELLED`.
- `inventory_service`: có dữ liệu sản phẩm/kho, persistence dùng R2DBC; cần bổ sung reservation và transaction reactive đúng cách.
- `payment_service`, `shipping_service`: hiện có danh mục phương thức; cần xây nghiệp vụ payment/shipment.
- `notification_service`: đang consume sự kiện order; cần giữ tương thích hoặc migrate rõ ràng khi đổi sự kiện.
- Tài liệu flow cũ trong `order_service/docs/todos/` có reserve đồng bộ. Kế hoạch này là hướng mới cho phần triển khai saga.

## 3. Luồng mục tiêu

Giả định giai đoạn đầu: thanh toán online. COD là nhánh nghiệp vụ bổ sung sau.

```text
POST /api/orders
  → Lưu order PENDING + saga WAITING_STOCK + outbox ReserveStock
  → Trả orderId + PENDING

Inventory nhận ReserveStock
  → Reserve kho + outbox StockReserved/StockRejected

Order nhận StockReserved
  → Saga WAITING_PAYMENT + outbox CreatePayment

Payment tạo phiên, chờ kết quả thanh toán thực
  → Outbox PaymentSucceeded/PaymentFailed

Order nhận PaymentSucceeded
  → Saga WAITING_STOCK_COMMIT + outbox CommitReservation

Inventory nhận CommitReservation
  → Chốt reservation + outbox ReservationCommitted

Order nhận ReservationCommitted
  → Order CONFIRMED + saga COMPLETED + outbox sự kiện xác nhận
  → Khởi tạo tiến trình fulfillment + outbox CreateShipment

Shipping xử lý CreateShipment
  → Outbox ShipmentCreated/ShipmentCreationFailed
  → Order cập nhật tiến trình fulfillment
```

Tạo shipment thuộc tiến trình fulfillment riêng, không đợi giao hàng xong mới xác nhận đơn. Shipping lỗi tạm thời được retry; không tự động hoàn tiền chỉ vì một lần tạo vận đơn lỗi. Chính sách hủy khi không thể giao cần được xác định trước khi bật nhánh đó.

## 4. Message contract và topic

Envelope chung:

| Field | Ý nghĩa |
|---|---|
| messageId | ID message; giữ nguyên khi outbox phát lại |
| messageType, schemaVersion | Loại message và phiên bản payload |
| sagaId, orderId | Liên kết tiến trình và đơn |
| correlationId | Với reply: messageId của command được trả lời |
| operationId | Business key cố định của thao tác, ví dụ reservationId |
| occurredAt | Thời điểm tạo message |
| payload | Dữ liệu nghiệp vụ và mã lỗi nếu có |

Topic ban đầu: `inventory.commands`, `payment.commands`, `shipping.commands`, `order.saga.replies`. Sự kiện nghiệp vụ cho notification dùng topic riêng, không dùng command để báo khách hàng đã đặt thành công.

Kafka key là orderId. Mỗi nhóm consumer nghiệp vụ có group ID ổn định. Không dựa vào thứ tự giữa các topic; Order vẫn kiểm tra state và command đang chờ.

## 5. Dữ liệu cần bổ sung

### Inbox tại từng service

`inbox_messages(consumer_name, message_id, processed_at)`.

- Primary key `(consumer_name, message_id)`.
- consumer_name là tên handler logic cố định, không phải instance ID.
- Insert bằng cơ chế conflict-safe, không SELECT trước rồi mới INSERT.
- Inbox, thay đổi nghiệp vụ và outbox phải commit/rollback cùng transaction local.
- Chỉ commit Kafka offset sau khi DB transaction hoàn tất; cấu hình listener phải bảo đảm không bỏ qua message lỗi hoặc chưa hoàn thành.

### Outbox tại từng service

`outbox_messages(id, aggregate_id, saga_id, correlation_id, message_type, schema_version, topic, payload, created_at, published_at, attempts, next_attempt_at, last_error)`.

- id cũng là messageId; aggregate_id là orderId.
- Index phục vụ tìm message chưa gửi và đã tới thời điểm retry.
- Publisher chỉ đánh dấu published sau broker ACK; lỗi hoặc ACK không rõ kết quả thì retry cùng ID.
- Claim bằng lock/lease khi chạy nhiều instance; nếu dùng lease cần owner, locked_until và cơ chế thu hồi sau crash.
- Không giữ DB transaction của nghiệp vụ trong lúc chờ Kafka.
- Có thể phát trùng khi crash sau broker ACK; inbox xử lý trường hợp đó.
- Nếu cần thứ tự publish theo aggregate phải thiết kế sequence/claim tương ứng; Kafka key riêng lẻ không sửa được thứ tự đã bị đảo ở publisher.

### Saga và nghiệp vụ

- Order: `order_sagas` chứa sagaId, orderId, state, version, expectedCommandId, deadline, reservationId, paymentId, lỗi gần nhất và timestamps.
- Mỗi order có một saga đặt hàng đang hoạt động; optimistic locking hoặc conditional update ngăn hai reply cùng chuyển state.
- Trạng thái order dành cho nghiệp vụ; trạng thái saga dành cho tiến trình. Bổ sung `CANCELLING` khi compensation chưa xong.
- Inventory: reservation có business key unique, items, trạng thái RESERVED/COMMITTED/RELEASED và thời hạn nếu áp dụng.
- Payment: payment operation và provider reference; unique business key, trạng thái thanh toán/refund.
- Shipping: shipment operation với business key unique và trạng thái fulfillment.
- Định nghĩa rõ available/reserved/sold theo schema kho thực tế để commit reservation không trừ kho hai lần.

## 6. Quy tắc state machine và lỗi

| State hiện tại | Kết quả hợp lệ | Hành động |
|---|---|---|
| WAITING_STOCK | StockReserved | Lưu reservation, chuyển WAITING_PAYMENT, ghi CreatePayment |
| WAITING_STOCK | StockRejected | Hủy đơn, kết thúc saga |
| WAITING_PAYMENT | PaymentSucceeded | Chuyển WAITING_STOCK_COMMIT, ghi CommitReservation |
| WAITING_PAYMENT | PaymentFailed xác định | Chuyển COMPENSATING, ghi ReleaseStock |
| WAITING_STOCK_COMMIT | ReservationCommitted | Xác nhận đơn, hoàn thành saga, khởi tạo fulfillment |
| COMPENSATING | Các reply bù trừ cần thiết đã hoàn tất | Chuyển order CANCELLED và kết thúc saga |

- Reply trùng: bỏ qua qua inbox. Cùng operation nhưng messageId mới: trả kết quả nghiệp vụ đã lưu, không làm lại side effect.
- Reply muộn/sai command: có quy tắc riêng; không chuyển bước chỉ dựa vào messageType.
- StockReserved đến sau khi bắt đầu hủy: bảo đảm gửi ReleaseStock.
- Timeout là chưa biết kết quả, không tự đồng nhất với thất bại. Lưu deadline trong DB, có worker phục hồi khi restart.
- Payment timeout: đối soát provider trước quyết định; PaymentSucceeded đến muộn sau yêu cầu hủy cần refund theo policy.
- Nếu đã thu tiền nhưng không thể hoàn tất đơn: theo dõi refund và trả kho cho tới khi hoàn tất; lỗi kéo dài chuyển xử lý thủ công.
- Release đến trước Reserve phải ngăn Reserve muộn tạo giữ kho mới, ví dụ lưu trạng thái hủy theo reservationId.
- Không tự hết hạn reservation đang được chốt mà thiếu cơ chế phối hợp; xử lý cuộc đua payment/expiry/commit bằng transition có điều kiện.
- Retry lỗi tạm thời có backoff; message sai schema hoặc lỗi không phục hồi đưa DLQ và cảnh báo. DLQ không có nghĩa saga đã xử lý xong.

## 7. Các bước triển khai tuần tự

### Bước 1 — Chốt contracts và transition

- [ ] Định nghĩa envelope, command/reply payload, mã lỗi, topic/group ID.
- [ ] Viết transition table gồm success, failure, timeout, duplicate, late reply và compensation.
- [ ] Xác định business key cho reservation, payment, refund, shipment.
- [ ] Chốt nguồn giá/phí phía server: API hiện nhận unitPrice/shippingFee từ client; không dùng trực tiếp làm số tiền thu thực tế nếu chưa kiểm chứng.
- [ ] Chốt thời điểm order confirmed và ranh giới fulfillment như mục 3.

Hoàn thành khi: contract có ví dụ JSON, mỗi transition có điều kiện và command đầu ra; chưa cần framework state machine riêng.

### Bước 2 — Outbox của Order

- [ ] Thêm Liquibase migration, entity/repository và port ghi outbox.
- [ ] Thay publish trực tiếp trong transaction nghiệp vụ bằng ghi outbox, giữ tương thích sự kiện hiện tại.
- [ ] Viết polling publisher có ACK, retry/backoff, claim an toàn và recovery.

Hoàn thành khi: Kafka tắt vẫn lưu được order + outbox; Kafka bật lại gửi được; crash/retry giữ nguyên messageId. Rollback DB không để lại message để publish.

### Bước 3 — Khởi tạo saga và API theo dõi

- [ ] Thêm order_sagas, state/version, expectedCommandId, deadline.
- [ ] POST order ghi order + saga WAITING_STOCK + ReserveStock trong một transaction.
- [ ] Thêm GET `/api/orders/{id}` trả trạng thái phù hợp cho frontend và kiểm tra quyền xem đơn.
- [ ] Thêm idempotency cho POST tạo đơn: key gắn người dùng + request fingerprint; cùng key/payload trả cùng order, khác payload bị từ chối.

Hoàn thành khi: gọi lại POST không tạo hai đơn; restart không mất tiến trình đang chờ.

### Bước 4 — Inventory reserve và reply

- [ ] Thêm reservation, inbox, outbox và publisher tại Inventory.
- [ ] Consume ReserveStock; reserve đủ tất cả items hoặc không reserve item nào.
- [ ] Dùng atomic update/locking phù hợp để không oversell khi nhiều đơn cạnh tranh.
- [ ] Gom inbox + reserve + reply outbox trong một reactive transaction R2DBC; listener chỉ hoàn tất sau transaction, không subscribe fire-and-forget.
- [ ] Chống trùng theo cả messageId và reservationId; operationId tái dùng với payload khác phải bị từ chối.

Hoàn thành khi: hai đơn tranh lượng hàng cuối không oversell; message trùng không giữ kho hai lần; rollback không để lại inbox hoặc reservation dở dang.

### Bước 5 — Order consume reply và chuyển state

- [ ] Thêm inbox Order và handler StockReserved/StockRejected.
- [ ] Trong cùng transaction: dedup + kiểm tra expected command/state/version + chuyển state + ghi command tiếp theo.
- [ ] Kiểm thử Payment bằng test fixture trước khi có service thực, không báo thanh toán thành công giả trong production.

Hoàn thành khi: chạy trọn vòng Order → Inventory → Order; nhận reply hai lần chỉ tạo một thao tác Payment; reply sai/muộn không làm saga đi sai bước.

### Bước 6 — Payment request/reply

- [ ] Thêm payment operation, inbox/outbox và CreatePayment handler.
- [ ] Tạo session thanh toán; phân biệt PaymentSessionCreated với PaymentSucceeded.
- [ ] Callback có xác thực, chống trùng, đối soát và lưu kết quả trước khi phát reply.
- [ ] Dùng operationId ổn định làm provider idempotency key khi provider hỗ trợ; có reconciliation nếu kết quả gọi provider chưa rõ.
- [ ] Không coi inbox/DB transaction là bảo vệ được side effect tại provider.

Hoàn thành khi: retry không thu tiền hai lần; callback trùng/đến muộn và provider timeout đều có kết quả xử lý xác định.

### Bước 7 — Commit kho, compensation và timeout

- [ ] Thêm CommitReservation, ReleaseStock và các reply idempotent.
- [ ] Xác nhận order chỉ sau ReservationCommitted.
- [ ] Thêm RefundPayment và theo dõi compensation nếu cần trả tiền.
- [ ] Worker deadline bền vững, retry có giới hạn và trạng thái cần can thiệp.
- [ ] Kiểm tra race reserve/release, payment/timeout, commit/expiry và reply đến muộn.

Hoàn thành khi: các nhánh thất bại không bỏ quên kho/tiền; chỉ đánh dấu CANCELLED sau các tác vụ bù trừ bắt buộc đã hoàn tất.

### Bước 8 — Shipping và notification

- [ ] Thêm shipment operation, inbox/outbox và CreateShipment handler.
- [ ] Tách tiến trình fulfillment khỏi saga đặt hàng đã hoàn tất.
- [ ] Chống tạo vận đơn trùng bằng operationId/provider key và reconciliation.
- [ ] Retry lỗi tạo vận đơn; chốt đường xử lý thủ công/hủy nếu không thể giao.
- [ ] Migrate notification: phân biệt đơn tiếp nhận, đơn xác nhận, đơn hủy; chống gửi thông báo trùng.

Hoàn thành khi: Shipping ngừng tạm thời không làm mất đơn đã xác nhận; chạy lại không tạo vận đơn trùng.

### Bước 9 — Kiểm chứng toàn flow và vận hành

- [ ] Integration test với PostgreSQL và Kafka cho các transaction boundary thực.
- [ ] Test crash sau DB commit/trước offset commit, sau Kafka ACK/trước published_at, restart giữa các step.
- [ ] Test message trùng, message mới cùng operation, reply đảo thứ tự, cạnh tranh kho, timeout provider.
- [ ] Metrics/log theo orderId/sagaId/messageId: tuổi outbox, saga quá hạn, retry, DLQ, compensation pending.
- [ ] Runbook replay DLQ, retry operation, đối soát tiền/kho và xử lý thủ công.
- [ ] Retention inbox/outbox phù hợp cửa sổ replay; không xóa dấu dedup khi message vẫn có thể quay lại. Business key cần giữ theo vòng đời nghiệp vụ.

Hoàn thành khi: flow phục hồi sau các tình huống lỗi trên, không trừ kho/thu tiền/tạo vận đơn lặp, truy ra được đơn đang kẹt ở đâu.

## 8. Cách làm theo kế hoạch

Triển khai theo thứ tự 1 → 9. Mốc đầu tiên là hoàn thành bước 1–5 để chứng minh vòng Order ↔ Inventory hoạt động. Sau mỗi bước, cập nhật checkbox, ghi kiểm chứng và các quyết định mới ngay trong file này.

Chỉ dùng chung envelope/tiện ích kỹ thuật trong `common` nếu phù hợp; transaction, migrations và repository inbox/outbox vẫn thuộc từng service. Không ép Inventory R2DBC dùng transaction JPA của Order.

## 9. Nhật ký tiến độ

| Ngày | Kết quả | Kiểm chứng |
|---|---|---|
| 2026-11-05 | Tạo kế hoạch theo kiến trúc đã thống nhất; chưa sửa code nghiệp vụ | Đối chiếu cấu trúc và luồng hiện có trong repo |
