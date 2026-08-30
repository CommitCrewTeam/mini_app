# Roadmap: Hoàn thiện luồng Order Service (Checkout Flow)

## Bối cảnh hiện tại (đã có)
- `OrderAggregateRoot` + `OrderItem` + `MoneyValue` — domain layer cơ bản (create, addItem, confirm, cancel)
- `PlaceOrderApplicationService` (Application Service) + `CreateOrderUseCase` (port inbound)
- `OrderController` + `OrderRestMapper` (REST adapter, đã tách domain khỏi response)
- `OrderPersistenceAdapter` + `OrderEntity`/`OrderItemEntity` (JPA)
- `OrderKafkaProducer` (outbound messaging, cần soát lại có publish domain object trực tiếp không)
- `InventoryFeignAdapter` (outbound REST gọi Inventory)

## Vấn đề tồn đọng cần xử lý trước (Technical Debt)
Đây là nợ kỹ thuật đã phát hiện ở vòng review trước, nên dọn trước khi build tiếp feature mới, tránh nợ chồng nợ:

1. **Outbox Pattern** cho `publishOrderEventPort` — tránh dual-write problem (DB commit nhưng Kafka fail hoặc ngược lại)
2. **Optimistic Locking** (`@Version`) trên `OrderEntity` — tránh race condition khi 2 request sửa cùng Order
3. **Persistence Mapper diff theo item** thay vì rebuild toàn bộ `OrderItemEntity` mỗi lần save
4. Làm rõ `PlaceOrderDomainService` có logic thật hay chỉ pass-through thừa — nếu thừa thì xóa
5. Soát `OrderKafkaProducer` — có đang serialize thẳng `OrderAggregateRoot` lên Kafka không, nếu có thì phải tách Event DTO riêng (Published Language, không phải ACL)

---

## Giai đoạn 1 — Hoàn thiện Order Aggregate (nội bộ, chưa gọi service khác)

| Việc cần làm | Ghi chú |
|---|---|
| `updateItemQuantity`, `removeItem` đã có — thêm UseCase + Controller cho các thao tác này | Theo đúng nguyên tắc: load nguyên Order, gọi method, save lại nguyên Order |
| `CancelOrderUseCase` | Áp state machine đã định nghĩa: PENDING/CONFIRMED → CANCELLED |
| `GetOrderUseCase` (query) | Trả `OrderResponse`, không trả domain object |
| Domain Event: `OrderCreatedEvent`, `OrderItemQuantityChangedEvent`, `OrderCancelledEvent` | Event Mapper riêng trong Application Service, tách khỏi cấu trúc Aggregate |
| Outbox table + polling publisher (hoặc Debezium CDC) | Xử lý nợ kỹ thuật #1 ở trên |
| `@Version` field + migration | Xử lý nợ kỹ thuật #2 |

**Định nghĩa xong (Definition of Done) giai đoạn 1:** Tạo/sửa/hủy Order hoạt động độc lập, đúng invariant, publish event tin cậy (outbox), không còn thao tác nào bypass Aggregate.

---

## Giai đoạn 2 — Tích hợp Inventory (reserve/release stock)

**Câu hỏi thiết kế cần chốt trước khi code:** Reserve stock đồng bộ (gọi API trước khi tạo Order) hay bất đồng bộ (tạo Order trạng thái `PENDING`, Inventory tự reserve qua event, fail thì rollback)?

Khuyến nghị: **Đồng bộ ở bước reserve** (vì user cần biết ngay có hết hàng hay không tại thời điểm bấm đặt hàng), **bất đồng bộ ở bước trừ kho thật** (chỉ trừ hẳn khi Order `CONFIRMED`/thanh toán xong).

| Việc cần làm | Ghi chú |
|---|---|
| `InventoryPort.reserveStock(orderId, items)` — gọi đồng bộ qua Feign lúc checkout | Nếu 1 sản phẩm hết hàng → reject toàn bộ request, trả lỗi rõ ràng cho user |
| Trạng thái Order mới: `AWAITING_PAYMENT` (đã reserve, chưa thanh toán) | Cập nhật lại State Machine ở `OrderStatus` |
| `InventoryReservationExpiredEvent` (nếu user bỏ ngang, không thanh toán trong X phút) | Cần cơ chế timeout — dùng scheduler hoặc Kafka delay queue |
| Consumer nghe `OrderCancelledEvent`/`ReservationExpiredEvent` bên Inventory để tự release stock | Bên Inventory tự chịu trách nhiệm compensate, Order không gọi ngược lại release trực tiếp |

**Định nghĩa xong:** Checkout không tạo được Order nếu hết hàng; Order tồn đọng quá lâu không thanh toán tự động release kho.

---

## Giai đoạn 3 — Tích hợp Promotion (coupon Shopee + coupon Shop)

*(Cần tách `promotion_service` mới theo đề xuất trước, nếu chưa có trong repo thì tạo module mới)*

| Việc cần làm | Ghi chú |
|---|---|
| `PromotionPort.validateAndCalculate(orderId, couponCodes, orderContext)` | Order gửi context (subtotal, danh sách sản phẩm/shop) qua, Promotion trả về số tiền giảm + coupon hợp lệ/không |
| Xử lý stacking rule (2 coupon dùng chung được không) | Nằm bên trong `promotion_service`, Order không cần biết rule chi tiết |
| `Order` lưu lại: `appliedCouponIds`, `discountAmount` (kết quả trả về, không tự tính) | Order chỉ lưu kết quả, không tự implement logic giảm giá |
| Xử lý concurrency: coupon giới hạn số lượt dùng, 2 user cùng lúc dùng coupon cuối cùng | Promotion service tự lock/giảm số lượt còn lại, trả lỗi nếu hết lượt |
| Domain Event `CouponAppliedEvent`/`CouponRedeemFailedEvent` | Để Order xử lý fallback nếu coupon fail sau khi đã hiển thị cho user |

**Định nghĩa xong:** Checkout áp dụng đúng coupon hợp lệ, tổng tiền cuối cùng phản ánh đúng discount, không có race condition coupon bị dùng vượt giới hạn.

---

## Giai đoạn 4 — Tích hợp Shipping (tính phí, chọn carrier)

*(Cần tách `shipping_service` mới, mỗi carrier có ACL riêng — GhnAdapter, GhtkAdapter...)*

| Việc cần làm | Ghi chú |
|---|---|
| `ShippingPort.calculateFee(orderId, address, items)` | Gọi đồng bộ lúc checkout, trả về danh sách phương thức + phí tương ứng để user chọn |
| `Order` lưu `shippingMethodCode`, `shippingFee` (kết quả trả về) | Không tự tính phí ship trong Order domain |
| Sau khi Order `CONFIRMED` (đã thanh toán) → publish event, Shipping service tự tạo vận đơn thật (gọi carrier API) | Bất đồng bộ, không block luồng checkout |
| Xử lý lỗi tạo vận đơn thất bại (carrier API die) | Retry + dead-letter queue, KHÔNG rollback ngược lại Order đã thanh toán |

**Định nghĩa xong:** User thấy đúng phí ship theo địa chỉ/carrier đã chọn; vận đơn được tạo tự động sau khi Order xác nhận, có cơ chế retry khi carrier lỗi.

---

## Giai đoạn 5 — Tích hợp Payment

| Việc cần làm | Ghi chú |
|---|---|
| `PaymentPort.initiatePayment(orderId, amount, paymentMethodCode)` | Order chỉ khởi tạo giao dịch, không xử lý logic gateway |
| Webhook/callback từ Payment service → `OrderPaymentSucceededEvent` / `OrderPaymentFailedEvent` | Order consume event này để chuyển state `AWAITING_PAYMENT → CONFIRMED` hoặc `→ CANCELLED` |
| Idempotency cho webhook (payment gateway có thể gửi trùng callback) | Payment service tự xử lý, nhưng Order consumer cũng nên check `orderId` đã xử lý event này chưa |
| Timeout: user không thanh toán trong X phút | Tự động cancel Order + release Inventory + release Coupon đã áp dụng |

**Định nghĩa xong:** Order chuyển đúng trạng thái theo kết quả thanh toán thực tế, xử lý được các ca lỗi/timeout/trùng lặp webhook.

---

## Giai đoạn 6 — Saga / Orchestration tổng thể luồng Checkout

Sau khi từng tích hợp đơn lẻ ở Giai đoạn 2-5 đã chạy được, cần 1 lớp **điều phối toàn bộ luồng** (Saga), xử lý rollback nếu 1 bước giữa chừng fail:

```
Checkout Saga:
  1. Reserve Inventory        (fail -> dừng ngay, báo hết hàng)
  2. Validate + Apply Coupon  (fail -> release Inventory, báo lỗi coupon)
  3. Calculate Shipping Fee   (fail -> release Inventory + Coupon, báo lỗi)
  4. Create Order (PENDING/AWAITING_PAYMENT)
  5. Initiate Payment
  6. [Async] Payment callback -> CONFIRMED hoặc rollback toàn bộ (release Inventory, Coupon, Cancel Order)
```

| Việc cần làm | Ghi chú |
|---|---|
| Chọn mô hình Saga: Choreography (event-driven, mỗi service tự nghe/phản ứng) hay Orchestration (1 nhạc trưởng trung tâm điều phối) | Với luồng nhiều bước tuần tự + cần rollback rõ ràng như trên, **Orchestration dễ kiểm soát hơn** — cân nhắc thêm 1 `CheckoutOrchestrator` trong `order_service` hoặc tách riêng |
| Compensating transaction cho từng bước | Mỗi service (Inventory, Promotion, Shipping) tự expose thêm API/event "release/undo" |
| Idempotency key xuyên suốt cả saga (1 `sagaId`/`orderId` duy nhất) | Tránh xử lý trùng khi có retry ở bất kỳ bước nào |

**Định nghĩa xong:** Toàn bộ luồng checkout end-to-end chạy được, tự rollback đúng khi bất kỳ bước nào fail giữa chừng, không để lại data mồ côi (kho bị trừ nhưng Order không tồn tại, coupon bị dùng nhưng Order hủy...).

---

## Thứ tự ưu tiên đề xuất

1. **Dọn nợ kỹ thuật hiện tại** (Outbox, Version, Persistence diff) — nền móng phải chắc trước
2. **Giai đoạn 1** (hoàn thiện Order nội bộ) — vì mọi giai đoạn sau đều build trên nền này
3. **Giai đoạn 2** (Inventory) — vì đây là điều kiện chặn cứng (không có hàng thì không cho đặt)
4. **Giai đoạn 5** (Payment) — có thể làm song song hoặc trước Giai đoạn 3/4 nếu muốn có luồng checkout tối thiểu chạy end-to-end sớm (MVP: bỏ qua coupon/shipping phức tạp trước)
5. **Giai đoạn 3 + 4** (Promotion, Shipping) — bổ sung sau khi có luồng lõi chạy ổn
6. **Giai đoạn 6** (Saga orchestration) — làm sau cùng, khi đã rõ hết các API/event của từng service riêng lẻ

---

## Câu hỏi cần chốt trước khi code tiếp (để tránh code rồi sửa lại)

1. Reserve Inventory đồng bộ hay bất đồng bộ? (khuyến nghị: đồng bộ)
2. Saga dùng Orchestration hay Choreography? (khuyến nghị: Orchestration cho luồng checkout)
3. `promotion_service` và `shipping_service` — tách module Gradle riêng ngay từ đầu hay tạm code trong `order_service` rồi tách sau?
4. Outbox implement bằng polling job tự viết hay dùng Debezium CDC (cần thêm hạ tầng Kafka Connect)?
