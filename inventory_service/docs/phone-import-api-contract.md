# Phone Import API Contract

Đối chiếu với code ngày 2026-11-14. Service: `inventory_service`.

Base URL khi chạy local: `http://localhost:8082`.

## 1. Danh sách API

| Method | Path | Request | HTTP thành công | `data` trả về |
|---|---|---|---|---|
| POST | `/api/phones/imports` | Multipart: `file`, `mode` | 202 | `{ importId, jobExecutionId }`: hai ID int64 |
| POST | `/api/phones/imports/{importId}/validate` | Không có body | 202 | `jobExecutionId`: integer/int64 |
| POST | `/api/phones/imports/{importId}/approve` | Không có body | 202 | `{ importId, jobExecutionId }`: hai ID int64 |

`202 Accepted` chỉ xác nhận đã gửi job, không xác nhận xử lý thành công. `importId` và `jobExecutionId` là hai loại ID khác nhau; path luôn dùng ID int64 của import.

## 2. Response chung

Response là JSON theo `ApiResponse<T>`. `code` là string; response thành công dùng `"200"` trong body dù HTTP status là `202`. `timestamp` là thời gian UTC dạng ISO-8601.

```json
{
  "success": true,
  "code": "200",
  "message": "Phone import submitted",
  "data": { "importId": 10, "jobExecutionId": 100 },
  "timestamp": "2026-11-14T03:00:00Z"
}
```

## 3. Upload CSV

`POST /api/phones/imports`

Content-Type: `multipart/form-data`. Để HTTP client tự tạo multipart boundary.

| Part | Kiểu | Bắt buộc | Quy tắc |
|---|---|---|---|
| `file` | File | Có | Tên kết thúc bằng `.csv`, không phân biệt hoa thường; file không rỗng |
| `mode` | Text | Có | Chính xác `CREATE`, `UPDATE` hoặc `RESTOCK`; phân biệt hoa thường |

Giới hạn ứng dụng mặc định: **20,971,520 bytes (20 MiB)**, cấu hình bằng `PHONE_IMPORT_MAX_BYTES`. Multipart còn có giới hạn riêng: `max-disk-usage-per-part: 20MB`, `max-parts: 2`, `max-in-memory-size: 256KB`. Tăng giới hạn ứng dụng không tự tăng giới hạn multipart.

Ví dụ PowerShell, chạy tại thư mục gốc repository:

```powershell
curl.exe -i -X POST "http://localhost:8082/api/phones/imports" -F "file=@inventory_service/docs/example.csv;type=text/csv" -F "mode=CREATE"
```

HTTP `202`; response như mục 2, dùng `data.importId` để gọi validate/approve và xem `/stat`. Upload chạy ingest CSV và validation ở background; chưa ghi các draft vào bảng phone.

Upload và approve trả 202 sau khi service lưu PhoneImport và launcher nhận job. Submit thất bại làm import chuyển FAILED và request trả lỗi; file upload chưa được nhận xử lý được dọn dẹp. Job đã bắt đầu rồi mới thất bại được báo qua `/stat`.

Approve dùng executor riêng với `app.phone-import.approval.launch-workers` (mặc định 5) và `app.phone-import.approval.launch-queue-capacity` (mặc định 20). Hết chỗ thì từ chối submit, không chờ worker rảnh. Chưa có outbox để phục hồi trường hợp process chết giữa lưu trạng thái và submit.

## 4. Validate lại

`POST /api/phones/imports/{importId}/validate`

Path parameter `importId`: ID int64, bắt buộc. Không có body. Import phải ở trạng thái `READY`.

```powershell
$importId = 10
curl.exe -i -X POST "http://localhost:8082/api/phones/imports/$importId/validate"
```

HTTP `202`:

```json
{
  "success": true,
  "code": "200",
  "message": "Phone validation submitted",
  "data": 101,
  "timestamp": "2026-11-14T03:01:00Z"
}
```

Job kiểm tra lại các draft chưa `APPLIED`, gồm cả draft đã `VALID` hoặc `INVALID`. Dòng thiếu dữ liệu do lỗi parse vẫn giữ lỗi; API này không sửa CSV. Validation thành công và tất cả draft VALID đưa import tới `VALIDATED`; có dòng không hợp lệ hoặc job/launch lỗi thì kết thúc `FAILED`. Không cho validate lần thứ hai.

## 5. Approve

`POST /api/phones/imports/{importId}/approve`

Path parameter `importId`: ID int64, bắt buộc. Không có body. Import phải ở trạng thái `READY` hoặc `VALIDATED`.

```powershell
$importId = 10
curl.exe -i -X POST "http://localhost:8082/api/phones/imports/$importId/approve"
```

HTTP `202`:

```json
{
  "success": true,
  "code": "200",
  "message": "Phone approval submitted",
  "data": { "importId": 10, "jobExecutionId": 102 },
  "timestamp": "2026-11-14T03:02:00Z"
}
```

Server chuyển `READY / VALIDATED → APPROVING` với điều kiện trạng thái/version chưa thay đổi, rồi chạy validation lại trước khi áp dụng. Không bắt buộc gọi API validate riêng trước approve.

| Kết quả background | Trạng thái import | Tác động |
|---|---|---|
| Có draft không `VALID` | `FAILED` | Bị chặn trước bước ghi phone |
| Áp dụng hoàn tất | `DONE` | Các draft đã áp dụng thành `APPLIED` |
| Job lỗi | `FAILED` | Có thể đã áp dụng một phần |

Phone và trạng thái draft `APPLIED` được ghi trong cùng transaction của từng chunk. Chunk đã commit không rollback khi chunk sau lỗi. Không coi `FAILED` là chưa ghi dữ liệu. Job hiện cấu hình `preventRestart()`; không có API resume/retry cho import `FAILED`.

## 6. CSV và ý nghĩa mode

File UTF-8, dấu phân cách `,`, bốn cột đúng thứ tự sau. Reader bỏ dòng đầu tiên mà không kiểm tra tên header. Mỗi bản ghi nên nằm trên một dòng.

```csv
name,detail,active,stock
iPhone 15,"{""color"":""black"",""storage"":""256GB""}",false,10
Galaxy S24,"{""color"":""white""}",false,5
Pixel 8,"{""color"":""blue""}",false,0
```

| Cột | Quy tắc hiện tại |
|---|---|
| `name` | Không rỗng; dùng làm key sau khi trim và chuyển chữ thường. Key không được trùng trong cùng import |
| `detail` | JSON object, ví dụ `{}`. Bọc field bằng dấu nháy kép và escape nháy kép bên trong bằng `""` |
| `active` | Bắt buộc có vị trí cột, nhưng giá trị hiện không được dùng hoặc validate |
| `stock` | Số nguyên Java `Integer`, không âm; vẫn phải hợp lệ cho cả ba mode |

| Mode | Điều kiện key | Khi approve thành công |
|---|---|---|
| `CREATE` | Chưa tồn tại trong phone | Tạo phone với name/key đã chuẩn hóa, detail và stock từ CSV; `active=false` bất kể CSV |
| `UPDATE` | Đã tồn tại | Thay toàn bộ detail; giữ stock, name và active hiện tại |
| `RESTOCK` | Đã tồn tại | Cộng stock từ CSV vào stock hiện tại; giữ detail, name và active |

`detail` vẫn phải là JSON object trong RESTOCK, dù không được dùng để cập nhật phone. UPDATE/RESTOCK kiểm tra version để tránh ghi đè thay đổi đồng thời.

File mẫu: [example.csv](example.csv). CREATE chỉ hợp lệ nếu các key mẫu chưa tồn tại; UPDATE/RESTOCK cần các key đã tồn tại.

## 7. Lỗi HTTP và lỗi background

Lỗi `AppException` có dạng:

```json
{
  "success": false,
  "code": "CONFLICT",
  "message": "Import is not ready",
  "data": null,
  "timestamp": "2026-11-14T03:03:00Z"
}
```

| API / điều kiện | HTTP | Body code | Message |
|---|---|---|---|
| Upload: mode sai | 400 | `BAD_REQUEST` | `Mode must be CREATE, UPDATE or RESTOCK` |
| Upload: tên file sai | 400 | `BAD_REQUEST` | `Upload a .csv file` |
| Upload: file rỗng/quá giới hạn ứng dụng | 400 | `BAD_REQUEST` | `CSV must be nonempty and at most 20971520 bytes` (số bytes theo cấu hình) |
| Validate/approve: ID int64 không có trong DB | 404 | `NOT_FOUND` | `Phone import not found` |
| Validate: không READY; approve: không READY/VALIDATED | 409 | `CONFLICT` | `Import is not ready` |
| Approve: cập nhật trạng thái/version thất bại | 409 | `CONFLICT` | `Import changed or approval already started` |
| Exception chưa được ánh xạ riêng | 500 | `500` | `Something went wrong` |

DB chỉ cho một import chưa DONE/FAILED tại một thời điểm. Upload khi đang có import hoạt động có thể vi phạm unique constraint; code hiện chưa ánh xạ lỗi này thành 409. Tương tự, lỗi optimistic locking khi validate chưa được ánh xạ riêng.

Thiếu multipart part, ID int64 sai định dạng, hoặc vượt giới hạn multipart không có mapping riêng trong exception handler; không cam kết trả cùng mã/message với các lỗi ứng dụng ở bảng trên.

Lỗi từng dòng được lưu vào draft, không trả trong HTTP 202 đã gửi. Các message hiện có gồm `Cannot parse CSV columns`, `Cannot parse stock integer`, `Cannot parse detail JSON object`, `Key is required`, `Stock must not be negative`, `Duplicate key in import`, `Key already exists in phone`, `Key does not exist in phone`.

## 8. Trạng thái và giới hạn tích hợp hiện tại

Validate/approve dùng tên job cùng `importId` và `mode` để định danh JobInstance; không tạo ID int64 ngẫu nhiên mỗi lần launch. `approvalVersion` chỉ là tham số xử lý, không tham gia định danh. Batch từ chối launch lại instance đã hoàn tất, kể cả approve hoàn tất với kết quả bị validation chặn. API validate có thể chạy sau validation trong upload vì đó là hai job khác tên, nhưng không thể chạy lặp lại sau khi chính job validate đã hoàn tất. Job vẫn dùng `preventRestart()`. Các lỗi từ chối của Batch hiện chưa có mapping riêng nên có thể trả HTTP 500.

```text
Upload:   PENDING → READY / FAILED
Validate: READY → VALIDATING → VALIDATED / FAILED
Approve:  READY / VALIDATED → APPROVING → DONE / FAILED
```

`READY` nghĩa là job trước đã hoàn tất, không đảm bảo mọi draft hợp lệ. `VALIDATED` chỉ cho phép đi tiếp sang APPROVING. `DONE` và `FAILED` là trạng thái kết thúc; không có chuyển trạng thái quay lại hoặc tự chuyển về chính nó.

Hiện chỉ có ba endpoint ở mục 1. Chưa có API lấy trạng thái import/job, danh sách draft/lỗi, sửa draft, tải lỗi, hủy hoặc xóa import. Vì vậy client chưa thể hoàn thiện luồng polling chỉ bằng HTTP API hiện có. Khi kiểm thử local, cần kiểm tra DB/log để biết import đã READY trước khi gọi bước tiếp theo.

## 9. Case API theo ImportMode

Các lệnh dùng PowerShell, chạy ở thư mục gốc repository. Ba mode dùng chung endpoint upload; chỉ thay multipart part `mode` và file CSV. Mode được cố định lúc upload; validate/approve không nhận mode mới.

Chạy các case thành công theo thứ tự CREATE → UPDATE → RESTOCK. Trước case đầu, ba key mẫu phải chưa tồn tại và không có import đang hoạt động. Chỉ bắt đầu upload case tiếp theo sau khi import trước đã `DONE`. Các kết quả dưới đây giả định không có thao tác khác thay đổi phone trong lúc chạy.

### Case 1: CREATE — tạo phone mới

File: [example.csv](example.csv).

```powershell
$create = curl.exe -sS -X POST "http://localhost:8082/api/phones/imports" -F "file=@inventory_service/docs/example.csv;type=text/csv" -F "mode=CREATE" | ConvertFrom-Json
$create
$createImportId = $create.data.importId
```

Upload thành công trả HTTP `202`, `success=true`, `data` chứa `importId` và `jobExecutionId`. Kiểm tra `/stat` đến khi import `READY`, rồi chạy:

```powershell
curl.exe -i -X POST "http://localhost:8082/api/phones/imports/$createImportId/approve"
```

Approve trả HTTP `202` và job execution ID. Sau khi import `DONE`, kiểm tra kết quả:

```powershell
curl.exe -i "http://localhost:8082/api/phones"
```

Mong đợi có `iphone 15` stock `10`, `galaxy s24` stock `5`, `pixel 8` stock `0`; detail tương ứng CSV. Cả ba phone có `active=false`.

### Case 2: UPDATE — thay detail, giữ stock

File: [example-update.csv](example-update.csv). Dùng cùng ba key đã tạo ở case 1. Stock trong file cố ý là `999`, active là `true` để minh họa hai cột này không được dùng để cập nhật phone trong mode UPDATE.

```powershell
$update = curl.exe -sS -X POST "http://localhost:8082/api/phones/imports" -F "file=@inventory_service/docs/example-update.csv;type=text/csv" -F "mode=UPDATE" | ConvertFrom-Json
$update
$updateImportId = $update.data.importId
```

Sau khi import `READY`, có thể gọi validate riêng để kiểm thử API này:

```powershell
curl.exe -i -X POST "http://localhost:8082/api/phones/imports/$updateImportId/validate"
```

Validate trả HTTP `202` và job execution ID. Chờ import tới `VALIDATED` rồi approve:

```powershell
curl.exe -i -X POST "http://localhost:8082/api/phones/imports/$updateImportId/approve"
```

Sau khi `DONE`, gọi `GET /api/phones`. Mong đợi detail được thay bằng object trong file UPDATE, stock vẫn lần lượt `10`, `5`, `0`; active vẫn `false`. UPDATE thay toàn bộ object detail, không merge từng thuộc tính.

### Case 3: RESTOCK — cộng tồn kho

File: [example-restock.csv](example-restock.csv). Số lượng cộng lần lượt là `3`, `2`, `1`; detail dùng `{}` vì vẫn phải parse được JSON object.

```powershell
$restock = curl.exe -sS -X POST "http://localhost:8082/api/phones/imports" -F "file=@inventory_service/docs/example-restock.csv;type=text/csv" -F "mode=RESTOCK" | ConvertFrom-Json
$restock
$restockImportId = $restock.data.importId
```

Chờ import `READY`, rồi chạy:

```powershell
curl.exe -i -X POST "http://localhost:8082/api/phones/imports/$restockImportId/approve"
```

Sau khi `DONE`, gọi `GET /api/phones`. Mong đợi stock lần lượt `13`, `7`, `1`; detail giữ kết quả case UPDATE, active vẫn `false`.

### Case lỗi theo mode

Các case lỗi dữ liệu dưới đây chạy độc lập với dữ liệu/DB kiểm thử riêng. Import có dòng INVALID vẫn có thể ở READY và ngăn upload tiếp; hiện chưa có API hủy/xóa import.

| Case | Cách tạo | Kết quả mong đợi |
|---|---|---|
| CREATE key đã tồn tại | Upload `example.csv` bằng CREATE sau case 1 | Upload có thể trả 202; background đánh dấu draft INVALID: `Key already exists in phone` |
| UPDATE key chưa tồn tại | Upload `example-update.csv` bằng UPDATE trên DB không có ba key mẫu | Draft INVALID: `Key does not exist in phone` |
| RESTOCK key chưa tồn tại | Upload `example-restock.csv` bằng RESTOCK trên DB không có ba key mẫu | Draft INVALID: `Key does not exist in phone` |
| Stock âm, bất kỳ mode | Đổi stock một dòng thành `-1` | Draft INVALID: `Stock must not be negative` |
| Trùng key trong file, bất kỳ mode | Lặp lại một key trong CSV | Draft INVALID: `Duplicate key in import` |
| Sai tên mode | Gửi `mode=create` hoặc `mode=UPSERT` | HTTP 400, code `BAD_REQUEST`, message `Mode must be CREATE, UPDATE or RESTOCK` |

Với các case draft INVALID: gọi approve khi READY vẫn có thể nhận HTTP 202, nhưng validation gate chặn bước ghi phone và đưa import tới FAILED. Không mong đợi HTTP 400 cho lỗi từng dòng CSV.

Ví dụ kiểm tra mode sai, không tạo import:

```powershell
curl.exe -i -X POST "http://localhost:8082/api/phones/imports" -F "file=@inventory_service/docs/example.csv;type=text/csv" -F "mode=create"
```

## 10. Nguồn đối chiếu

- `PhoneImportController`: routes, request và response.
- `PhoneImportApplicationService`: kiểm tra file và giới hạn dung lượng.
- `SpringBatchPhoneImportJob`, `PhoneImportValidationJob`, `ApproveImportJob`: vòng đời xử lý.
- `PhoneDraftMapper`, `PhoneDraft`, `Phone`: parse CSV, validation và tác động theo mode.
- `ApiResponse`, `GlobalExceptionHandler`: envelope và mapping lỗi.
