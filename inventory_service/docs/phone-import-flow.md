# Thiết kế import phone: APPEND và UPDATE

*Luồng xử lý, state machine, validate V1/V2 và xử lý concurrency*

## 1. Tổng quan

User upload file CSV gồm `key`, `quantity` để thêm hoặc sửa bảng `phone`. Mode chọn lúc upload và áp cho cả file: **APPEND** (chỉ thêm key mới) hoặc **UPDATE** (chỉ sửa key đã có). Rule nghiệp vụ duy nhất của import: **key không được trùng**.

| Bảng | Cột chính | Vai trò |
|---|---|---|
| `phone_import` | `id`, `status`, `mode`, `version`, `created_by`, `message`, `error_rows` | Header, 1 dòng cho mỗi lần upload. Giữ state machine của cả lần import. |
| `phone_draft` | `import_id`, `row_number`, `key`, `quantity`, `status`, `error_code`, `matched_id`, `matched_version` | Bảng nháp, 1 dòng cho mỗi dòng file. Giữ state từng dòng. |
| `phone` | `id`, `key` (UNIQUE), `quantity`, `version`, `created_at` | Bảng thật. Chỉ bị ghi ở bước Apply. |

## 2. Luồng tổng

```mermaid
flowchart TD
    A["1 Upload (file + mode) → PENDING"] --> B["2 Parse + V1 → VALID / INVALID"]
    B --> C["3 V2 resolve: tra phone theo key"]
    B --> R["REJECTED: file lỗi nặng / lỗi quá ngưỡng"]
    C --> R
    C --> D["4 READY: chờ user approve"]
    D --> E["5 Approve (CAS) → APPLYING"]
    E --> F["6 V2 verify: tra lại theo key"]
    F -. "có CONFLICT → về READY" .-> D
    F --> G["APPEND: INSERT … DO NOTHING"]
    F --> H["UPDATE: WHERE id AND version"]
    G --> I["7 Tổng kết: DONE + error_rows"]
    H --> I
```

*Hình 1. Luồng import từ upload đến hoàn tất. Trong bản PDF, màu vàng: cần user xử lý; màu xanh: ghi vào phone.*

| # | Việc làm | `phone_import` | `phone_draft` |
|---|---|---|---|
| 1 | Upload file và chọn mode. Có import khác chưa kết thúc thì trả 409. | tạo mới PENDING | chưa có |
| 2 | Parse CSV vào draft và chạy V1. Sai header hoặc lỗi quá ngưỡng thì dừng. | VALIDATING, hoặc FAILED | PARSED → VALID / INVALID |
| 3 | V2 resolve: tra phone theo key, gán kết quả vào draft. | VALIDATING | VALID giữ nguyên, hoặc INVALID kèm `error_code`; UPDATE lưu `matched_id`, `matched_version` |
| 4 | Tổng hợp lỗi, user xem preview. Cần sửa file thì hủy và upload lại. | READY (`version + 1`) | |
| 5 | Approve kèm version, CAS READY → APPLYING. | APPLYING | |
| 6 | V2 verify: tra lại. Có CONFLICT thì không ghi gì, quay về bước 4. | APPLYING → READY, `version + 1` | dòng lệch: CONFLICT |
| 7 | Apply theo chunk (không gọi V2). | APPLYING | APPLIED, FAILED hoặc SKIPPED |
| 8 | Tổng kết. | DONE kèm `error_rows` | chờ dọn theo TTL |

## 3. Hai mức validate

### V1: soft validate, chỉ nhìn file

- Size file, header, kiểu dữ liệu, trùng key trong file. Chạy một lần lúc parse.
- `key` phải được normalize (trim, lowercase...) đúng theo cách `phone.key` đang lưu, nếu không so sánh sẽ lệch.
- Dòng đạt là VALID, dòng lỗi là INVALID.

### V2: heavy validate, tra bảng phone (chỉ rule key)

Đọc draft theo chunk 500 đến 1000 dòng, mỗi chunk một query, không query từng dòng:

```sql
SELECT id, key, version FROM phone WHERE key IN (:keys_of_chunk);
```

**Resolve** chạy một lần sau V1 và ghi kết quả vào draft:

| Kết quả tra key | Mode APPEND | Mode UPDATE |
|---|---|---|
| Chưa có trong phone | OK, sẽ INSERT | INVALID (`KEY_NOT_FOUND`) |
| Đã có trong phone | INVALID (`KEY_EXISTS`) | OK, lưu `matched_id` và `matched_version` |

**Verify** chạy lại mỗi lần user approve, để bắt thay đổi của phone từ lúc resolve:

| Mode | Verify thấy gì | Kết quả |
|---|---|---|
| APPEND | key giờ đã có trong phone | CONFLICT (`KEY_EXISTS`) |
| UPDATE | key không còn trong phone | CONFLICT (đã bị xóa) |
| UPDATE | version khác `matched_version` | CONFLICT (có người sửa giữa chừng) |

Lúc Apply không gọi V2. Nếu READY để quá lâu (ví dụ hơn 24 giờ) thì buộc resolve lại trước khi cho approve.

## 4. State machine

```mermaid
flowchart LR
    P[PENDING] --> V[VALIDATING]
    V --> R[READY]
    R --> A[APPLYING]
    A --> D[DONE]
    A -. "lệch version (CONFLICT)" .-> R
    V -->|file lỗi nặng| F["FAILED (message)"]
    R -->|hủy / hết hạn| F
    A -->|lỗi hệ thống| F
```

*Hình 2. State machine của `phone_import`.*

| State | Ý nghĩa |
|---|---|
| PENDING | Vừa upload, chờ worker nhận. |
| VALIDATING | Đang parse, chạy V1 và V2 resolve. |
| READY | Validate xong, chờ user approve. Không ai ghi gì. |
| APPLYING | Đang verify rồi ghi vào phone. |
| DONE | Xong. Số dòng lỗi nằm ở `error_rows`. |
| FAILED | Dữ liệu bị từ chối, hệ thống lỗi, user hủy hoặc hết hạn. Lý do ở cột `message`. |

### State của từng dòng trong `phone_draft`

| State | Ý nghĩa và chuyển tiếp |
|---|---|
| PARSED | Vừa ghi từ file. Sau V1/V2 thành VALID hoặc INVALID. |
| VALID | Đủ điều kiện apply. Apply xong thành APPLIED hoặc FAILED. Verify lệch thì thành CONFLICT. |
| INVALID | Kết thúc, không apply, nằm trong error report. |
| CONFLICT | Verify thấy phone đã đổi. User chọn bỏ qua (SKIPPED) hoặc ghi đè (về VALID). |
| APPLIED / FAILED / SKIPPED | Kết quả cuối của dòng. |

## 5. Apply

### Mode APPEND

```sql
INSERT INTO phone (key, quantity, version, created_at)
VALUES ('A', ...), ('B', ...), ('C', ...), ('D', ...)
ON CONFLICT (key) DO NOTHING
RETURNING key;
```

- Ví dụ: phone đã có sẵn key B. RETURNING trả về A, C, D. B bị DB bỏ qua và không có trong kết quả.
- Dòng có key trong RETURNING thì APPLIED. Dòng không có thì FAILED với `KEY_EXISTS`.
- Cả chunk vẫn chạy trong một câu lệnh, một dòng trùng không làm hỏng các dòng khác.
- Không dùng DO UPDATE (upsert), vì sẽ âm thầm ghi đè dữ liệu có sẵn, trái với ý nghĩa mode APPEND.
- Điều kiện: V1 đã loại trùng key trong file, và phone với phone_draft cùng một DB để ghi phone và cập nhật draft chung một transaction.

### Mode UPDATE

```sql
UPDATE phone
SET quantity = :quantity, version = version + 1
WHERE id = :matched_id AND version = :matched_version;
```

- Trúng 1 dòng: APPLIED.
- Trúng 0 dòng: có người sửa chen vào sau verify. Dòng đó FAILED, các dòng khác vẫn chạy.
- Verify lệch thì user chọn cho từng dòng CONFLICT: bỏ qua (SKIPPED), hoặc ghi đè (gán `matched_version` bằng version hiện tại của phone, dòng về VALID), hoặc hủy cả import. Sau đó approve lại.

## 6. Concurrency

Trọng tài là DB, không dùng lock trong memory, nên chạy đúng với nhiều pod. Chỉ cần ba chỗ:

| Chỗ | Rủi ro | Cách chặn |
|---|---|---|
| Upload | Hai import chạy cùng lúc | Partial unique index, import thứ hai nhận 409 |
| Approve | Double-click, hai admin cùng bấm, bấm trên preview cũ | CAS kèm version của import |
| Ghi vào phone | Có người tạo hoặc sửa key giữa verify và apply | UNIQUE (`key`) cho APPEND, WHERE `version = ...` cho UPDATE |

```sql
CREATE UNIQUE INDEX uq_one_active_import ON phone_import ((1))
  WHERE status NOT IN ('DONE', 'FAILED');

UPDATE phone_import SET status = 'APPLYING', version = version + 1
WHERE id = :id AND status = 'READY' AND version = :clientVersion;
-- 0 dòng thì trả 409
```

Khi chạy nhiều pod:

- File upload lưu ở chỗ dùng chung (S3/MinIO hoặc volume chung), không để trên disk local của pod.
- Mọi chuyển state đi qua một hàm dùng `UPDATE ... WHERE status = :from`. Trả 0 dòng nghĩa là pod khác đã xử lý.
- Job nền viết theo kiểu CAS để nhiều pod cùng chạy cũng vô hại. Dùng `now()` của DB để tính timeout.
- Với Spring Batch: `importId` là UUID ngẫu nhiên mỗi lần launch nên JobRepository không chặn trùng. Việc chặn do `phone_import` đảm nhiệm.

## 7. API

| API | Mục đích |
|---|---|
| `POST /imports (file, mode)` | Tạo import, trả 202 kèm `importId`. 409 nếu có import chưa kết thúc. |
| `GET /imports/{id}` | Trạng thái, version và số liệu tổng quan. Client poll. |
| `GET /imports/{id}/rows?status=&page=` | Preview, danh sách lỗi và CONFLICT. Phân trang, tải được error report. |
| `POST /imports/{id}/rows/{rowNo}/resolve (SKIP \| FORCE)` | User chọn cách xử lý cho dòng CONFLICT. |
| `POST /imports/{id}/approve (version)` | Chuyển READY → APPLYING. 409 nếu sai state hoặc version cũ. |
| `POST /imports/{id}/cancel` | Hủy khi chưa vào APPLYING. |

Trả `importId` (UUID) ra ngoài, không trả `jobExecutionId` của Spring Batch. Trạng thái trả về là state của `phone_import`, không phải BatchStatus.

## 8. Job nền

- Quét import kẹt ở VALIDATING hoặc APPLYING quá lâu (theo `updated_at` hoặc heartbeat), đưa về state trước đó hoặc FAILED.
- READY quá hạn thì chuyển FAILED để nhả mutex.
- Dọn `phone_draft` và file upload sau N ngày.

## 9. Ranh giới với nghiệp vụ phone

- State của import nằm ở `phone_import` và `phone_draft`. State nghiệp vụ của phone do phone tự quản. Không đặt cột kiểu `import_status` vào phone.
- Dòng được tạo ra phải có state khởi tạo đúng nghiệp vụ, ghi rõ ở bước Apply, không để mặc định ngầm.
- Muốn truy vết thì thêm cột `created_by_import_id` (cho phép NULL) hoặc bảng audit. Không dùng nó để điều khiển nghiệp vụ.
- Sau Apply phát sự kiện cho các phần khác của inventory (reindex, cache...), tốt nhất qua outbox. Từ đó import không còn liên quan.
- Apply bằng SQL trực tiếp bỏ qua domain service của phone. Chấp nhận được vì rule duy nhất là key. Nếu sau này phone có thêm rule thì đưa vào V1/V2 hoặc cho Apply đi qua domain service.
