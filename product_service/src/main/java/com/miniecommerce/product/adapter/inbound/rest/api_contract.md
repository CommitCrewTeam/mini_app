# Product Cache API Contract

Controller: `ProductCacheController`  
Service: `product_service`  
Base URL: `http://localhost:8085/api/v1/cache-demo/products`

Các lệnh dưới đây chạy trực tiếp trong Bash/Git Bash/WSL, không cần khai báo biến hay cài `jq`. Khởi động service và Redis theo cấu hình ứng dụng trước khi gọi. Controller không khai báo yêu cầu token.

Dữ liệu demo nằm trong `MOCK_DATABASE` (bộ nhớ của service), có sẵn ID `101`, `102`, `103` khi khởi động. Độ trễ truy vấn được giả lập 200 ms; các API này không truy vấn PostgreSQL. Redis cache `product_detail` có TTL 10 phút theo `RedisConfig`.

## Tổng quan endpoint

| Method | Path (sau base URL) | Chức năng | Kiểu `data` |
| --- | --- | --- | --- |
| GET | `/{id}` | Lấy sản phẩm, dùng `@Cacheable` | `ProductCacheResponse` |
| GET | `/{id}/benchmark` | Lấy sản phẩm và thông tin cache | `CacheBenchmarkResponse` |
| PUT | `/{id}` | Ghi sản phẩm vào bộ nhớ và cache | `ProductCacheResponse` |
| DELETE | `/{id}` | Xóa sản phẩm và cache tương ứng | string |
| DELETE | `/clear-all` | Xóa toàn bộ cache `product_detail` | string |

`id` là số nguyên kiểu Java `Long`. Tất cả kết quả được controller trả về bình thường đều có HTTP **200**, kể cả khi body chứa `code: "404"`. Controller hiện không dùng `ResponseEntity` để đổi HTTP status cho trường hợp không tìm thấy.

## Response chung

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {},
  "timestamp": "2026-11-08T08:00:00Z"
}
```

`timestamp` là thời điểm tạo response (`Instant`, ISO-8601 theo cấu hình JSON mặc định). Các thời gian và số đo dưới đây chỉ là ví dụ.

### ProductCacheResponse

| Field | Kiểu JSON | Ý nghĩa |
| --- | --- | --- |
| `id` | number | ID sản phẩm (`Long`) |
| `name` | string | Tên sản phẩm |
| `price` | number | Giá (`BigDecimal`) |
| `description` | string | Mô tả |
| `stock` | number | Số lượng (`Integer`) |
| `cachedAt` | string | Thời điểm tạo dữ liệu cache, dạng `yyyy-MM-dd HH:mm:ss`, theo giờ server |

## 1. GET /{id} — Lấy sản phẩm

```bash
curl -sS -w '\nHTTP %{http_code}\n' \
  'http://localhost:8085/api/v1/cache-demo/products/101'
```

Response HTTP 200:

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "id": 101,
    "name": "MacBook Pro M3 Max 16-inch",
    "price": 79990000,
    "description": "Apple M3 Max chip with 16-core CPU and 40-core GPU",
    "stock": 25,
    "cachedAt": "2026-11-08 15:00:00"
  },
  "timestamp": "2026-11-08T08:00:00Z"
}
```

Cache MISS: đọc bộ nhớ demo sau độ trễ giả lập, rồi ghi Redis. Cache HIT: trả dữ liệu đã cache; `cachedAt` giữ nguyên cho đến khi dữ liệu được nạp lại hoặc cập nhật.

Không tìm thấy sản phẩm (áp dụng cả GET benchmark):

```bash
curl -sS -w '\nHTTP %{http_code}\n' \
  'http://localhost:8085/api/v1/cache-demo/products/999999'
```

Response **HTTP 200**, body báo lỗi:

```json
{
  "success": false,
  "code": "404",
  "message": "Không tìm thấy sản phẩm với ID: 999999",
  "data": null,
  "timestamp": "2026-11-08T08:00:00Z"
}
```

## 2. GET /{id}/benchmark — Đo Cache MISS/HIT

```bash
curl -sS -w '\nHTTP %{http_code}\n' \
  'http://localhost:8085/api/v1/cache-demo/products/101/benchmark'
```

Response HTTP 200, ví dụ Cache MISS:

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "product": {
      "id": 101,
      "name": "MacBook Pro M3 Max 16-inch",
      "price": 79990000,
      "description": "Apple M3 Max chip with 16-core CPU and 40-core GPU",
      "stock": 25,
      "cachedAt": "2026-11-08 15:00:00"
    },
    "source": "DATABASE (Cache MISS)",
    "executionTimeMs": 205,
    "databaseLoadReduction": "0%",
    "explanation": "Lần đầu truy vấn: Hệ thống phải đọc từ Database (tốn ~200ms). Dữ liệu đã được tự động lưu vào Redis Cache cho các lần sau!"
  },
  "timestamp": "2026-11-08T08:00:00Z"
}
```

| Field trong `data` | Kiểu | Cache MISS | Cache HIT |
| --- | --- | --- | --- |
| `product` | object | `ProductCacheResponse` | `ProductCacheResponse` |
| `source` | string | `DATABASE (Cache MISS)` | `REDIS_CACHE (Cache HIT)` |
| `executionTimeMs` | number (`long`) | Thời gian thực thi đo được | Thời gian thực thi đo được |
| `databaseLoadReduction` | string | `0%` | `99%` |
| `explanation` | string | Giải thích việc nạp cache | Giải thích việc đọc cache |

`databaseLoadReduction` là chuỗi cố định trong code, không phải tỷ lệ được đo thực tế. `executionTimeMs` đo lời gọi lấy sản phẩm qua proxy, không gồm bước kiểm tra cache trước đó hay toàn bộ thời gian HTTP. Nhãn `DATABASE` trong demo chỉ dữ liệu bộ nhớ và độ trễ giả lập.

Để quan sát MISS rồi HIT, chạy nguyên khối sau (ID 101 phải còn tồn tại):

```bash
# Xóa cache trước khi đo; giữ nguyên dữ liệu sản phẩm.
curl -sS -w '\nHTTP %{http_code}\n' -X DELETE \
  'http://localhost:8085/api/v1/cache-demo/products/clear-all'

# Lần 1: dự kiến Cache MISS.
curl -sS -w '\nHTTP %{http_code}\n' \
  'http://localhost:8085/api/v1/cache-demo/products/101/benchmark'

# Lần 2: dự kiến Cache HIT.
curl -sS -w '\nHTTP %{http_code}\n' \
  'http://localhost:8085/api/v1/cache-demo/products/101/benchmark'
```

## 3. PUT /{id} — Cập nhật sản phẩm và cache

Request body: JSON tương ứng `ProductCacheResponse`. Gửi các trường nghiệp vụ như sau:

```bash
curl -sS -w '\nHTTP %{http_code}\n' -X PUT \
  'http://localhost:8085/api/v1/cache-demo/products/101' \
  -H 'Content-Type: application/json' \
  --data-raw '{
    "name": "MacBook Pro M3 Max 16-inch",
    "price": 75990000,
    "description": "Apple M3 Max chip with 16-core CPU and 40-core GPU",
    "stock": 30
  }'
```

`id` lấy từ URL; `cachedAt` do server tạo. Hai trường này trong body bị bỏ qua nếu được gửi. DTO và controller hiện không khai báo validation bắt buộc/giá trị tối thiểu cho các trường nghiệp vụ. PUT dùng thao tác `put` nên cũng tạo sản phẩm nếu ID chưa tồn tại.

Response HTTP 200:

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "id": 101,
    "name": "MacBook Pro M3 Max 16-inch",
    "price": 75990000,
    "description": "Apple M3 Max chip with 16-core CPU and 40-core GPU",
    "stock": 30,
    "cachedAt": "2026-11-08 15:05:00"
  },
  "timestamp": "2026-11-08T08:05:00Z"
}
```

Gọi GET hoặc benchmark sau PUT sẽ đọc dữ liệu vừa cập nhật trong cache, nếu cache chưa bị xóa/hết hạn.

## 4. DELETE /{id} — Xóa sản phẩm và cache

```bash
curl -sS -w '\nHTTP %{http_code}\n' -X DELETE \
  'http://localhost:8085/api/v1/cache-demo/products/101'
```

Response HTTP 200:

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": "Đã xóa sản phẩm ID: 101 và xóa Cache thành công!",
  "timestamp": "2026-11-08T08:00:00Z"
}
```

ID không tồn tại vẫn trả thành công. Sau khi xóa, GET ID đó trả body `code: "404"`. Có thể chạy lại PUT phía trên để tạo lại ID 101.

## 5. DELETE /clear-all — Xóa toàn bộ cache sản phẩm

```bash
curl -sS -w '\nHTTP %{http_code}\n' -X DELETE \
  'http://localhost:8085/api/v1/cache-demo/products/clear-all'
```

Response HTTP 200:

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": "Đã làm sạch toàn bộ Cache 'product_detail' thành công!",
  "timestamp": "2026-11-08T08:00:00Z"
}
```

Chỉ xóa namespace `product_detail`, giữ nguyên bộ nhớ sản phẩm và các namespace Redis khác. Lần GET tiếp theo của sản phẩm còn tồn tại sẽ nạp lại cache.

## Phạm vi lỗi

Ví dụ lỗi `404` trong tài liệu là nhánh nghiệp vụ do controller trả về. Path ID sai định dạng, JSON không hợp lệ hoặc lỗi kết nối Redis có thể tạo HTTP lỗi qua Spring; controller này không định nghĩa response riêng cho các lỗi đó.
