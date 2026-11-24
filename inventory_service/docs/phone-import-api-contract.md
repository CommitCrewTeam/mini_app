# Phone Import API Contract

Đối chiếu với code ngày 2026-11-14. Service: `inventory_service`.

Base URL khi chạy local: `http://localhost:6004`.

## 1. Danh sách API

| Method | Path | Request | HTTP thành công | `data` trả về |
|---|---|---|---|---|
| POST | `/api/v1/phones/imports` | Multipart: `file`, `mode` | 202 | `importId`: integer/int64 |
| POST | `/api/v1/phones/imports/{importId}/validate` | Không có body | 202 | `jobExecutionId`: integer/int64 |
| POST | `/api/v1/phones/imports/{importId}/approve` | JSON: `warningSkip` (boolean, tùy chọn; mặc định `false`) | 202 | `{ importId, jobExecutionId }`: hai ID int64 |

`202 Accepted` chỉ xác nhận đã gửi job, không xác nhận xử lý thành công. `importId` và `jobExecutionId` là hai loại ID khác nhau; path luôn dùng ID int64 của import.

## 2. Response chung

Response là JSON theo `ApiResponse<T>`. `code` là string; response thành công dùng `"200"` trong body dù HTTP status là `202`. `timestamp` là thời gian UTC dạng ISO-8601.

```json
{
  "success": true,
  "code": "200",
  "message": "Phone import submitted",
  "data": 10,
  "timestamp": "2026-11-14T03:00:00Z"
}
```

## 3. Upload CSV

`POST /api/v1/phones/imports`

Content-Type: `multipart/form-data`. Để HTTP client tự tạo multipart boundary.

| Part | Kiểu | Bắt buộc | Quy tắc |
|---|---|---|---|
| `file` | File | Có | Tên kết thúc bằng `.csv`, không phân biệt hoa thường; file không rỗng |
| `mode` | Text | Có | Chính xác `CREATE`, `UPDATE` hoặc `RESTOCK`; phân biệt hoa thường |

Giới hạn ứng dụng mặc định: **20,971,520 bytes (20 MiB)**, cấu hình bằng `PHONE_IMPORT_MAX_BYTES`. Multipart còn có giới hạn riêng: `max-disk-usage-per-part: 20MB`, `max-parts: 2`, `max-in-memory-size: 256KB`. Tăng giới hạn ứng dụng không tự tăng giới hạn multipart.

Ví dụ PowerShell, chạy tại thư mục gốc repository:

```powershell
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports" -F "file=@inventory_service/docs/example.csv;type=text/csv" -F "mode=CREATE"
```

HTTP `202`; response như mục 2, dùng `data.importId` để gọi validate/approve và xem `/stat`. Upload chạy ingest CSV và validation ở background; chưa ghi các draft vào bảng phone.

Upload, validate và approve trả 202 sau khi service lưu/cập nhật PhoneImport và launcher nhận job. Upload submit thất bại làm import chuyển `UPLOAD_FAILED`; validate/approve submit thất bại được ghi vào trạng thái failed tương ứng và request trả lỗi. Job đã bắt đầu rồi mới thất bại được báo qua `/stat`.

Approve dùng executor riêng với `app.phone-import.approval.launch-workers` (mặc định 5) và `app.phone-import.approval.launch-queue-capacity` (mặc định 20). Hết chỗ thì từ chối submit, không chờ worker rảnh. Chưa có outbox để phục hồi trường hợp process chết giữa lưu trạng thái và submit.

## 4. Validate lại

`POST /api/v1/phones/imports/{importId}/validate`

Path parameter `importId`: ID int64, bắt buộc. Không có body. Import phải ở trạng thái `UPLOAD_SUCCEEDED`.

```powershell
$importId = 10
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports/$importId/validate"
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

Job kiểm tra lại các draft chưa `APPLIED`, gồm cả draft đã `VALID` hoặc `INVALID`. Dòng thiếu dữ liệu do lỗi parse vẫn giữ lỗi; API này không sửa CSV. Validation thành công khi có ít nhất một draft và tất cả draft là VALID hoặc WARNING, đưa import tới `VALIDATE_SUCCEEDED`; có dòng không hợp lệ hoặc job/launch lỗi thì kết thúc `VALIDATE_FAILED`. Không cho validate lần thứ hai.

## 5. Approve

`POST /api/v1/phones/imports/{importId}/approve`

Path parameter `importId`: ID int64, bắt buộc. Import phải ở trạng thái `VALIDATE_SUCCEEDED`. Import ở `UPLOAD_SUCCEEDED` cần gọi API validate trước khi approve.

Body tùy chọn; nếu gửi thì có field `warningSkip`:

```json
{ "warningSkip": false }
```

Nếu không có draft `WARNING`, `warningSkip` có thể là `false` hoặc không gửi body. Nếu có warning, chỉ `warningSkip: true` mới cho phép áp dụng các draft đó. Với `false`, job dừng trước khi ghi phone và import trở lại `VALIDATE_SUCCEEDED` để người dùng xem warning rồi gửi approve lại. Draft `INVALID` luôn chặn approve, kể cả khi `warningSkip` là `true`.

Khi validate mode cần Phone có sẵn, lần validate đầu lưu `Phone.version` vào `matchedVersion`. Nếu một lần validate sau thấy version hiện tại đã đổi, draft chuyển thành `WARNING` và giữ baseline cũ. `warning_count` đếm các draft này. Approve với `warningSkip=false` dừng để người dùng xem warning; với `warningSkip=true`, draft WARNING được áp dụng lên Phone version hiện tại. Thay đổi Phone xảy ra sau khi approval đọc snapshot vẫn bị optimistic locking phát hiện.

```powershell
$importId = 10
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports/$importId/approve" -H "Content-Type: application/json" -d '{"warningSkip":false}'
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

Server chuyển `VALIDATE_SUCCEEDED → APPROVE_IN_PROGRESS` với điều kiện trạng thái/version chưa thay đổi, rồi chạy validation lại trước khi áp dụng. `warningSkip` là identifying job parameter; gửi `false` rồi `true` tạo hai JobInstance khác nhau để xác nhận và áp dụng warning.

| Kết quả background | Trạng thái import | Tác động |
|---|---|---|
| Có draft `INVALID` | `APPROVE_VALIDATION_FAILED` | Bị chặn trước bước ghi phone |
| Có draft `WARNING`, `warningSkip=false` | `VALIDATE_SUCCEEDED` | Bị chặn; cho phép gửi approve lại sau khi người dùng xem warning |
| Có draft `WARNING`, `warningSkip=true` | `APPROVE_SUCCEEDED` nếu áp dụng thành công | Warning được chấp nhận và áp dụng |
| Có draft và tất cả đều `VALID` | `APPROVE_SUCCEEDED` | Các draft đã áp dụng thành `APPLIED` |
| Import không có draft | `APPROVE_VALIDATION_FAILED` | Không có dữ liệu để áp dụng |
| Job lỗi | `APPROVE_FAILED` | Có thể đã áp dụng một phần |

Khi warning yêu cầu xác nhận, Batch JobExecution có thể kết thúc `COMPLETED` với exit code `APPROVAL_WARNING_CONFIRMATION_REQUIRED`; client cần xem `phone_import.status` (`VALIDATE_SUCCEEDED`) để biết chưa có phone nào được áp dụng.

Import không có draft được xem là không hợp lệ để approve; điều kiện này giữ nguyên hành vi trước đó yêu cầu import phải có ít nhất một draft.

Phone và trạng thái draft `APPLIED` được ghi trong cùng transaction của từng chunk. Chunk đã commit không rollback khi chunk sau lỗi. Không coi `FAILED` là chưa ghi dữ liệu. Job hiện cấu hình `preventRestart()`; không có API resume/retry cho import `APPROVE_FAILED`.

## 6. CSV và ý nghĩa mode

File UTF-8, dấu phân cách `,`, ba cột đúng thứ tự sau. Reader bỏ dòng đầu tiên mà không kiểm tra tên header. Mỗi bản ghi nên nằm trên một dòng. `rowNumber` không phải cột trong file: đó là số dòng do reader gán cho mỗi bản ghi.

```csv
key,detail,quantity
iPhone 15,"{""color"":""black"",""storage"":""256GB""}",10
Galaxy S24,"{""color"":""white""}",5
Pixel 8,"{""color"":""blue""}",0
```

| Cột | Quy tắc hiện tại |
|---|---|
| `key` | Không rỗng; được trim và chuyển chữ thường. Key không được trùng trong cùng import |
| `detail` | JSON object, ví dụ `{}`. Bọc field bằng dấu nháy kép và escape nháy kép bên trong bằng `""` |
| `quantity` | Số nguyên Java `Integer`, không âm; vẫn phải hợp lệ cho cả ba mode |

| Mode | Điều kiện key | Khi approve thành công |
|---|---|---|
| `CREATE` | Chưa tồn tại trong phone | Tạo phone với key đã chuẩn hóa, detail và quantity (thành stock) từ CSV; `active=false` bất kể CSV |
| `UPDATE` | Đã tồn tại | Thay toàn bộ detail; giữ stock, key và active hiện tại |
| `RESTOCK` | Đã tồn tại | Cộng quantity từ CSV vào stock hiện tại; giữ detail, key và active |

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
| Validate: không UPLOAD_SUCCEEDED; approve: không VALIDATE_SUCCEEDED | 409 | `CONFLICT` | `Import cannot be approved` |
| Approve: cập nhật trạng thái/version thất bại | 409 | `CONFLICT` | `Import changed or approval already started` |
| Exception chưa được ánh xạ riêng | 500 | `500` | `Something went wrong` |

DB chỉ cho một import chưa hoàn tất tại một thời điểm. Upload khi đang có import hoạt động có thể vi phạm unique constraint; code hiện chưa ánh xạ lỗi này thành 409. Tương tự, lỗi optimistic locking khi validate chưa được ánh xạ riêng.

Thiếu multipart part, ID int64 sai định dạng, hoặc vượt giới hạn multipart không có mapping riêng trong exception handler; không cam kết trả cùng mã/message với các lỗi ứng dụng ở bảng trên.

Lỗi từng dòng được lưu vào draft, không trả trong HTTP 202 đã gửi. Các message hiện có gồm `Cannot parse CSV columns`, `Cannot parse quantity integer`, `Cannot parse detail JSON object`, `Key is required`, `Stock must not be negative`, `Duplicate key in import`, `Key already exists in phone`, `Key does not exist in phone`.

## 8. Trạng thái và giới hạn tích hợp hiện tại

Validate/approve dùng tên job cùng identifying parameters để định danh JobInstance. Với approve, `warningSkip` phân biệt lần gửi chưa xác nhận (`false`) và lần chấp nhận warning (`true`). `preventRestart()` chặn restart cùng JobInstance; transition import sang `APPROVE_IN_PROGRESS` chặn approve đồng thời hoặc khi import không còn ở `VALIDATE_SUCCEEDED`. API validate có thể chạy sau validation trong upload vì đó là hai job khác tên, nhưng không thể chạy lặp lại sau khi chính job validate đã hoàn tất. Các lỗi từ chối của Batch hiện chưa có mapping riêng nên có thể trả HTTP 500.

```text
Upload:   UPLOAD_IN_PROGRESS → UPLOAD_SUCCEEDED / UPLOAD_FAILED
Validate: UPLOAD_SUCCEEDED → VALIDATE_IN_PROGRESS → VALIDATE_SUCCEEDED / VALIDATE_FAILED
Approve:  VALIDATE_SUCCEEDED → APPROVE_IN_PROGRESS → APPROVE_SUCCEEDED / APPROVE_FAILED / APPROVE_VALIDATION_FAILED
                         ↘ VALIDATE_SUCCEEDED (warning confirmation required)
```

`UPLOAD_SUCCEEDED` nghĩa là upload job đã hoàn tất; nó không khẳng định draft hợp lệ và chỉ cho phép bắt đầu validate. `VALIDATE_SUCCEEDED` là trạng thái duy nhất cho phép approve. Nếu cần xác nhận warning, lần approve đầu trả import về `VALIDATE_SUCCEEDED`, rồi có thể submit lại với `warningSkip=true`. `UPLOAD_FAILED`, `VALIDATE_FAILED`, `APPROVE_FAILED` và `APPROVE_VALIDATION_FAILED` là trạng thái kết thúc.

Hiện chỉ có ba endpoint ở mục 1. Chưa có API lấy trạng thái import/job, danh sách draft/lỗi, sửa draft, tải lỗi, hủy hoặc xóa import. Vì vậy client chưa thể hoàn thiện luồng polling chỉ bằng HTTP API hiện có. Khi kiểm thử local, cần kiểm tra DB/log để biết import đã `UPLOAD_SUCCEEDED` trước khi gọi validate.

## 9. Case API theo ImportMode

Các lệnh dùng PowerShell, chạy ở thư mục gốc repository. Ba mode dùng chung endpoint upload; chỉ thay multipart part `mode` và file CSV. Mode được cố định lúc upload; validate/approve không nhận mode mới.

Chạy các case thành công theo thứ tự CREATE → UPDATE → RESTOCK. Trước case đầu, ba key mẫu phải chưa tồn tại và không có import đang hoạt động. Chỉ bắt đầu upload case tiếp theo sau khi import trước đã `APPROVE_SUCCEEDED`. Các kết quả dưới đây giả định không có thao tác khác thay đổi phone trong lúc chạy.

### Case 1: CREATE — tạo phone mới

File: [example.csv](example.csv).

```powershell
$create = curl.exe -sS -X POST "http://localhost:6004/api/v1/phones/imports" -F "file=@inventory_service/docs/example.csv;type=text/csv" -F "mode=CREATE" | ConvertFrom-Json
$create
$createImportId = $create.data.importId
```

Upload thành công trả HTTP `202`, `success=true`, `data` chứa `importId` và `jobExecutionId`. Kiểm tra `/stat` đến khi import `UPLOAD_SUCCEEDED`, gọi validate, rồi đợi `/stat` tới `VALIDATE_SUCCEEDED`:

```powershell
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports/$createImportId/validate"
```

Sau khi `/stat` báo `VALIDATE_SUCCEEDED`, approve:

```powershell
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports/$createImportId/approve"
```

Approve trả HTTP `202` và job execution ID. Sau khi import `APPROVE_SUCCEEDED`, kiểm tra kết quả:

```powershell
curl.exe -i "http://localhost:6004/api/v1/phones"
```

Mong đợi có `iphone 15` stock `10`, `galaxy s24` stock `5`, `pixel 8` stock `0`; detail tương ứng CSV. Cả ba phone có `active=false`.

### Case 2: UPDATE — thay detail, giữ stock

File: [example-update.csv](example-update.csv). Dùng cùng ba key đã tạo ở case 1. `quantity` trong file cố ý là `999` để minh họa cột này không được dùng để cập nhật phone trong mode UPDATE.

```powershell
$update = curl.exe -sS -X POST "http://localhost:6004/api/v1/phones/imports" -F "file=@inventory_service/docs/example-update.csv;type=text/csv" -F "mode=UPDATE" | ConvertFrom-Json
$update
$updateImportId = $update.data.importId
```

Sau khi import `UPLOAD_SUCCEEDED`, có thể gọi validate riêng để kiểm thử API này:

```powershell
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports/$updateImportId/validate"
```

Validate trả HTTP `202` và job execution ID. Chờ import tới `VALIDATE_SUCCEEDED` rồi approve:

```powershell
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports/$updateImportId/approve"
```

Sau khi `APPROVE_SUCCEEDED`, gọi `GET /api/v1/phones`. Mong đợi detail được thay bằng object trong file UPDATE, stock vẫn lần lượt `10`, `5`, `0`; active vẫn `false`. UPDATE thay toàn bộ object detail, không merge từng thuộc tính.

### Case 3: RESTOCK — cộng tồn kho

File: [example-restock.csv](example-restock.csv). Số lượng cộng lần lượt là `3`, `2`, `1`; detail dùng `{}` vì vẫn phải parse được JSON object.

```powershell
$restock = curl.exe -sS -X POST "http://localhost:6004/api/v1/phones/imports" -F "file=@inventory_service/docs/example-restock.csv;type=text/csv" -F "mode=RESTOCK" | ConvertFrom-Json
$restock
$restockImportId = $restock.data.importId
```

Sau upload, gọi validate và chờ import `VALIDATE_SUCCEEDED`, rồi chạy:

```powershell
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports/$restockImportId/approve"
```

Sau khi `APPROVE_SUCCEEDED`, gọi `GET /api/v1/phones`. Mong đợi stock lần lượt `13`, `7`, `1`; detail giữ kết quả case UPDATE, active vẫn `false`.

### Case lỗi theo mode

Các case lỗi dữ liệu dưới đây chạy độc lập với dữ liệu/DB kiểm thử riêng. Import có dòng INVALID vẫn ở `UPLOAD_SUCCEEDED` sau upload, nhưng validate sẽ thất bại nên không thể approve; hiện chưa có API hủy/xóa import.

| Case | Cách tạo | Kết quả mong đợi |
|---|---|---|
| CREATE key đã tồn tại | Upload `example.csv` bằng CREATE sau case 1 | Upload có thể trả 202; background đánh dấu draft INVALID: `Key already exists in phone` |
| UPDATE key chưa tồn tại | Upload `example-update.csv` bằng UPDATE trên DB không có ba key mẫu | Draft INVALID: `Key does not exist in phone` |
| RESTOCK key chưa tồn tại | Upload `example-restock.csv` bằng RESTOCK trên DB không có ba key mẫu | Draft INVALID: `Key does not exist in phone` |
| Quantity âm, bất kỳ mode | Đổi quantity một dòng thành `-1` | Draft INVALID: `Stock must not be negative` |
| Trùng key trong file, bất kỳ mode | Lặp lại một key trong CSV | Draft INVALID: `Duplicate key in import` |
| Sai tên mode | Gửi `mode=create` hoặc `mode=UPSERT` | HTTP 400, code `BAD_REQUEST`, message `Mode must be CREATE, UPDATE or RESTOCK` |

Với các case draft INVALID: validate sẽ không đưa import tới `VALIDATE_SUCCEEDED`, vì vậy approve bị từ chối với HTTP 409. Không mong đợi HTTP 400 cho lỗi từng dòng CSV.

Ví dụ kiểm tra mode sai, không tạo import:

```powershell
curl.exe -i -X POST "http://localhost:6004/api/v1/phones/imports" -F "file=@inventory_service/docs/example.csv;type=text/csv" -F "mode=create"
```

## 10. Nguồn đối chiếu

- `PhoneImportController`: routes, request và response.
- `PhoneImportApplicationService`: kiểm tra file và giới hạn dung lượng.
- `SpringBatchPhoneImportJob`, `PhoneImportValidationJob`, `ApproveImportJob`: vòng đời xử lý.
- `PhoneDraftMapper`, `PhoneDraft`, `Phone`: parse CSV, validation và tác động theo mode.
- `ApiResponse`, `GlobalExceptionHandler`: envelope và mapping lỗi.
