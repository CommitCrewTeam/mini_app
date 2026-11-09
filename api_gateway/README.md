# API Gateway

Spring Cloud Gateway (reactive/WebFlux) cho hệ thống mini ecommerce.

## Chạy

Từ root project:

```powershell
.\gradlew.bat :api_gateway:bootRun
```

Gateway chạy tại: `http://localhost:8080`

Có thể override URL từng service bằng biến môi trường:

```powershell
$env:AUTH_SERVICE_URL = "http://localhost:8091"
$env:USER_SERVICE_URL = "http://localhost:8087"
.\gradlew.bat :api_gateway:bootRun
```

## Routes

| Route | Gateway path | Service | Port |
|---|---|---|---|
| auth-service | `/api/v1/auth/**` | auth_service | 8091 |
| auth-user-roles | `/api/v1/users/*/roles` | auth_service | 8091 |
| user-service | `/api/v1/users/**` | user_service | 8087 |
| order-service | `/api/v1/orders/**` | order_service | 8083 |
| inventory-service | `/api/v1/phones/**` | inventory_service | 8082 |
| payment-service | `/api/v1/payment-methods/**` | payment_service | 8088 |
| shipping-service | `/api/v1/shipping-methods/**` | shipping_service | 8084 |

> Gateway bỏ hai segment `/api/v1` trước khi forward request; controller trong
> từng service chỉ khai báo path service như `/users`, `/orders` hoặc `/phones`.
> Route `auth-user-roles` được khai báo **trước** route `user-service` để
> `/api/v1/users/{userId}/roles` đi đúng sang auth_service.
> Tạo user chỉ được thực hiện qua endpoint nội bộ `/internal/users` của
> user_service; endpoint này không được expose qua gateway.

## Kiểm tra

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/actuator/gateway/routes
```

Test đăng ký qua gateway:

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tuan123",
    "password": "password123",
    "firstName": "Tuan",
    "lastName": "Nguyen",
    "email": "tuan123@example.com",
    "address": "HCM",
    "birthDate": "2000-01-01"
  }'
```

Test lấy danh sách user qua gateway:

```bash
curl -i http://localhost:8080/api/v1/users
```

## Kiến trúc (Hexagonal / Ports & Adapters)

```
com.miniecommerce.gateway
├── domain/                        # Core – không phụ thuộc Spring/HTTP
│   ├── model/                     # TokenVerification, TokenStatus
│   └── port/
│       ├── in/                    # AuthenticateTokenUseCase (driving port)
│       └── out/                   # TokenVerificationPort (driven port)
├── application/                   # TokenAuthenticationService (use case impl)
└── adapter/
    ├── in/web/                    # AuthGlobalFilter, GatewayErrorResponseWriter
    └── out/http/                  # AuthHttpClient, WebClientConfig, HttpTokenVerificationAdapter
```

- `domain` và `application` không import Spring Gateway/WebFlux/WebClient.
- Filter (`adapter/in/web`) chỉ phụ thuộc input port và xử lý HTTP concern.
- Mọi kiến thức về auth_service (URL, header, DTO) nằm trong `adapter/out/http`.
- Response lỗi dùng chung format `ApiResponse` với `GlobalExceptionHandler` của module common.

## Xác thực

`AuthGlobalFilter` chặn mọi request trừ:

- `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/refresh`
- `/actuator`, `/actuator/**`
- CORS preflight (`OPTIONS`)

Token được gửi sang auth_service qua `POST /internal/auth/verify` (timeout 3s):

| Trường hợp | HTTP status |
|---|---|
| Token hợp lệ | request được forward |
| Thiếu header / token sai, hết hạn | `401 Unauthorized` |
| auth_service lỗi hoặc timeout | `503 Service Unavailable` |

## Ghi chú

- Gateway xác thực tập trung bằng filter + auth_service; chưa dùng OAuth2 Resource Server.
- Chưa dùng service discovery (Eureka); route đang trỏ trực tiếp bằng URL.
- Nếu chạy service trong Docker network, đổi `AUTH_SERVICE_URL`... thành
  `http://auth_service:8091`, `http://user_service:8087`...
