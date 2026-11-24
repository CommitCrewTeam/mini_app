# Prompt: Tạo dữ liệu test các case quan trọng của PhoneImport

Bạn hãy tạo bộ dữ liệu CSV nhỏ, có thể dùng ngay để kiểm tra flow import điện thoại của mini_app. Chỉ tập trung vào các case quan trọng dưới đây; không viết code test tự động, không mở rộng sang CRUD sản phẩm hoặc liệt kê hàng chục case phụ.

## 1. API và cách chạy

Base URL qua gateway: `http://localhost:6002/inventory_service/api/v1`.

- Upload: `POST /phones/imports`, body `multipart/form-data` gồm `file` (file CSV) và `mode` (`CREATE`, `UPDATE`, `RESTOCK`). Response trả `importId` trong `data`, HTTP 202.
- Xem trạng thái: `GET /phones/imports/{importId}/stat`.
- Validate lại: `POST /phones/imports/{importId}/validate`, không có body, HTTP 202 khi được nhận.
- Approve: `POST /phones/imports/{importId}/approve`, body JSON tùy chọn `{"warningSkip":false}` hoặc `{"warningSkip":true}`, HTTP 202 khi được nhận.
- Xem dữ liệu Phone sau approve: `GET /phones`.

Upload tự động chạy validation nhưng chưa ghi vào bảng Phone. Chỉ approve mới áp dụng dữ liệu. Các job chạy bất đồng bộ: sau mỗi thao tác, poll `/stat` khoảng 1 giây/lần, tối đa 60 giây; đợi kết thúc rồi mới gọi bước tiếp theo. HTTP 202 không đồng nghĩa xử lý thành công. Nếu hết thời gian chờ, báo timeout và ghi trạng thái cuối.

## 2. CSV và quy tắc nghiệp vụ

CSV UTF-8, dấu phẩy, đúng ba cột và đúng thứ tự:

```csv
key,detail,quantity
review01-iphone-15,"{""name"":""iPhone 15"",""color"":""black"",""storage"":""128GB""}",10
review01-galaxy-s24,"{""name"":""Galaxy S24"",""color"":""gray""}",5
```

- `key`: mã điện thoại, không rỗng; DB giới hạn 255 ký tự. Backend trim và chuyển về chữ thường. Các key trùng sau chuẩn hóa bị coi là duplicate.
- `detail`: chuỗi JSON object hợp lệ; `{}` cũng hợp lệ. Các thuộc tính như name, color, storage chỉ là dữ liệu mẫu, không phải schema bắt buộc. Trong CSV, bọc JSON bằng dấu nháy kép và escape mỗi dấu `"` thành `""` như ví dụ.
- `quantity`: số nguyên từ 0 đến 2147483647. Giá trị 0 là hợp lệ, không xếp vào invalid.
- Cả ba mode hiện đều yêu cầu parse được `detail` và `quantity`, kể cả khi không sử dụng trường đó để cập nhật.
- `CREATE`: key chưa tồn tại; tạo Phone với `stock = quantity`, `active = false`, ID do server sinh.
- `UPDATE`: key đã tồn tại; thay toàn bộ `detail`, giữ stock và active.
- `RESTOCK`: key đã tồn tại; cộng quantity vào stock, giữ detail và active.
- Duplicate key trong cùng import hoặc sai điều kiện tồn tại của mode làm draft INVALID.
- Một draft INVALID khiến cả import không đạt validation; không có cơ chế bỏ qua INVALID bằng warningSkip.
- Với UPDATE/RESTOCK, validation lưu baseline Phone.version. Nếu Phone đổi sau baseline, revalidate đánh dấu draft WARNING. WARNING vẫn cho import đạt VALIDATE_SUCCEEDED, nhưng approve cần warningSkip=true.
- `/stat` trả `total_count`, `fail_count`, `valid_count`, `warning_count`, `mode`, `status`. Không suy diễn có trường applied_count hoặc API sửa draft.

## 3. Chỉ tạo dữ liệu cho các case sau

### A. CREATE thành công và quantity bằng 0

Tạo `01-create-valid.csv` gồm 3 điện thoại, stock lần lượt 10, 5 và 0. Dùng prefix key `review01-` để dễ nhận diện. Hướng dẫn upload CREATE, đợi VALIDATE_SUCCEEDED, approve, đợi APPROVE_SUCCEEDED rồi kiểm tra Phone được tạo với stock đúng và active=false.

### B. UPDATE và RESTOCK thành công

Dùng key từ case A:

- `02-update-valid.csv`: thay detail của điện thoại stock 10; dùng quantity=0 và kiểm tra stock vẫn là 10 sau approve.
- `03-restock-valid.csv`: cộng 7 cho điện thoại stock 5; kiểm tra stock sau approve là 12 và detail không đổi dù file chứa detail khác.

Ghi rõ dữ liệu kỳ vọng trước/sau, không chỉ kiểm tra status.

### C. Dữ liệu không hợp lệ

Tạo `04-invalid-data.csv` với 5 dòng độc lập, key mới, mỗi dòng chỉ vi phạm một điều kiện:

1. Key chỉ chứa khoảng trắng.
2. Quantity=-1.
3. Quantity=1.5.
4. Quantity=2147483648.
5. Detail không phải JSON object hợp lệ, ví dụ `[]`.

Giữ cấu trúc CSV đúng để lỗi phản ánh đúng trường cần kiểm tra. Kỳ vọng upload CREATE kết thúc VALIDATE_FAILED, fail_count=5, không tạo Phone. Approve từ trạng thái này bị từ chối 409.

### D. Duplicate sau chuẩn hóa key

Tạo `05-duplicate-normalized.csv` gồm 2 dòng có key `review01-duplicate` và ` REVIEW01-DUPLICATE `. Các trường khác hợp lệ. Upload CREATE: cả hai dòng INVALID, fail_count=2, trạng thái VALIDATE_FAILED.

### E. Sai điều kiện tồn tại của mode

- `06-create-existing.csv`: một key đã được tạo ở case A; upload CREATE phải VALIDATE_FAILED.
- `07-existing-required.csv`: một key chắc chắn chưa tồn tại; dùng file này cho hai import riêng, UPDATE và RESTOCK; cả hai phải VALIDATE_FAILED.

### F. Validate thủ công hai lần trên cùng import — regression quan trọng

Tạo `08-revalidate.csv` với một key mới và dữ liệu hợp lệ. Chạy:

1. Upload CREATE, đợi VALIDATE_SUCCEEDED.
2. Gọi `/validate` lần đầu, đợi VALIDATE_SUCCEEDED.
3. Gọi `/validate` lần hai, đợi kết thúc.

Kết quả đúng: vẫn VALIDATE_SUCCEEDED, không có Phone mới vì chưa approve.

Bug đã xác định trong phiên bản chưa thêm tham số job `attempt`: lần gọi `/validate` thứ hai có thể được nhận 202 nhưng sau đó import chuyển VALIDATE_FAILED do trùng JobInstance đã hoàn thành. Đây là kết quả tái hiện bug, không phải hành vi đúng. Không tính validation tự động của upload là lần gọi `/validate` đầu tiên. Khi kiểm tra sau khi sửa, dùng import mới vì import đã FAILED hiện không được validate lại.

### G. Phone thay đổi sau validation → WARNING → xác nhận approve

Tạo hai file `09-warning-a.csv` và `10-warning-b.csv`, cùng trỏ tới điện thoại stock 10 ở case A, với detail khác nhau dễ nhận diện (ví dụ marker A và B).

Chạy đúng thứ tự, không cần SQL hoặc API cập nhật giả định:

1. Upload A với mode UPDATE, đợi VALIDATE_SUCCEEDED, chưa approve.
2. Upload B với mode UPDATE, đợi VALIDATE_SUCCEEDED, approve B và đợi APPROVE_SUCCEEDED. Phone.version lúc này đã thay đổi, detail là B.
3. Gọi `/validate` của A lần đầu, đợi VALIDATE_SUCCEEDED, kiểm tra warning_count=1.
4. Approve A với warningSkip=false, đợi quay về VALIDATE_SUCCEEDED; detail của Phone vẫn là B.
5. Approve A với warningSkip=true, đợi APPROVE_SUCCEEDED; detail trở thành A, stock không đổi.

Giải thích rõ warningSkip=true cho phép áp dụng WARNING, không bỏ qua dòng đó và không cho phép INVALID.

## 4. Output bắt buộc

1. Bảng ngắn gồm: mã case, tên file, mode, tiền điều kiện, thứ tự thao tác, kết quả mong đợi.
2. Nội dung đầy đủ của đúng 10 file CSV nói trên, mỗi file trong một code block riêng, có tên file ngay phía trên. Không dùng dấu `...` hoặc dữ liệu placeholder trong CSV.
3. Hướng dẫn Postman ngắn dùng chung cho upload, poll stat và approve; ghi rõ Content-Type khi gửi JSON, để Postman tự đặt multipart boundary khi upload.
4. Chỉ dẫn chạy A → B → C → D → E → F → G; các bước phụ thuộc dữ liệu phải đợi bước trước hoàn tất. Để chạy lại cả bộ, đổi đồng bộ prefix `review01-` trong tất cả file thành một prefix mới, giữ nguyên quan hệ giữa các key.

Chỉ tạo dữ liệu và hướng dẫn kiểm tra. Không gọi endpoint `/product_service/api/v1/products`, không dùng CSV `name,quantity`, không coi HTTP 202 là kết quả cuối, không bịa endpoint hoặc trường response chưa nêu.
