# Inventory Reservation — Design Doc

> Áp dụng cho `inventory_service` + `order_service` trong repo `mini_app`.
> Dựa trên bài blog "Inventory Reservation System Design" + thảo luận thực tế.

---

## 1. Bối cảnh & mục tiêu

Chống oversell (bán vượt tồn kho) khi nhiều request cùng lúc giữ chỗ (reserve) cùng 1 SKU.

Giả định:
- Strict no-oversell (không chấp nhận bán vượt)
- Hold time: 15 phút
- Single region
- Mỗi service tự quản DB riêng (microservices), **không share DB, không dùng FOREIGN KEY xuyên service**

---

## 2. Phân chia dữ liệu theo service

### `inventory_service` — sở hữu 2 bảng

```sql
-- Core: tồn kho
CREATE TABLE inventory (
    sku           VARCHAR(50) PRIMARY KEY,
    qty_total     INT NOT NULL,
    qty_avail     INT NOT NULL CHECK (qty_avail >= 0),
    qty_reserved  INT NOT NULL,
    version       INT NOT NULL DEFAULT 1
);
-- Bất biến: qty_total = qty_avail + qty_reserved

-- Giữ chỗ
CREATE TABLE reservations (
    reservation_id  UUID PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    sku             VARCHAR(50) REFERENCES inventory(sku),
    quantity        INT NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING', -- PENDING, CONFIRMED, EXPIRED, RELEASED
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW(),
    INDEX (expires_at),
    INDEX (user_id, sku)
);
```

### `order_service` — sở hữu 1 bảng

```sql
CREATE TABLE orders (
    order_id        UUID PRIMARY KEY,
    reservation_id  UUID NOT NULL,   -- KHÔNG FOREIGN KEY, khác DB
    user_id         BIGINT NOT NULL,
    status          VARCHAR(20) DEFAULT 'CREATED', -- CREATED, PAID, CANCELLED, FAILED
    created_at      TIMESTAMP DEFAULT NOW()
);
```

---

## 3. API (inventory_service expose ra ngoài)

```
POST   /reservations              -- tạo hold 15 phút
POST   /reservations/:id/confirm  -- chuyển sang CONFIRMED (sau khi payment ok)
DELETE /reservations/:id          -- release sớm
GET    /reservations/:id          -- check status
GET    /inventory/:sku            -- check tồn kho
```

Mọi request mutating (POST/DELETE) **bắt buộc** có header `Idempotency-Key`.
Lưu kết quả theo key trong 24h (Redis hoặc bảng riêng), retry thì trả cache, không chạy lại business logic.

---

## 4. Luồng giao tiếp giữa 2 service

### 4.1 Đồng bộ — lúc tạo order

```
order_service --(REST/gRPC call)--> POST /reservations (inventory_service)
inventory_service --(trả về)--> { reservation_id, expires_at }
order_service lưu reservation_id vào bảng orders
```

### 4.2 Bất đồng bộ — khi reservation hết hạn (qua Kafka)

```
inventory_service --(Kafka topic: reservation.expired)--> { reservation_id }
order_service consume --> nếu order.status == CREATED --> update thành CANCELLED
```

### 4.3 Bất đồng bộ — khi thanh toán xong (qua Kafka, hướng ngược lại)

```
order_service --(Kafka topic: order.paid)--> { reservation_id, order_id }
inventory_service consume --> gọi nội bộ /reservations/:id/confirm
```

---

## 5. Chống oversell — Optimistic locking

```sql
-- Đọc: qty_avail = 10, version = 5

UPDATE inventory
SET qty_avail = qty_avail - :qty, version = version + 1
WHERE sku = :sku
  AND version = :version
  AND qty_avail >= :qty;
-- Check affected rows:
--   1 row  -> thành công
--   0 rows -> conflict (version đã đổi) hoặc hết hàng -> đọc lại, retry hoặc reject
```

Giới hạn số lần retry (VD: 3 lần) để tránh loop vô hạn khi contention cao.

---

## 6. Giải phóng hàng hết hạn — Redis sorted set + Cron fallback

### 6.1 Redis sorted set (delay queue chính)

Khi tạo reservation:

```
ZADD reservation_expiry {expire_at_epoch} {reservation_id}
```

Worker (chạy trong `inventory_service`, 1 thread/scheduler riêng) poll liên tục (VD mỗi 1–5s):

```
ZRANGEBYSCORE reservation_expiry 0 {now_epoch}
```

Với mỗi `reservation_id` lấy được:
1. Kiểm tra `status == PENDING` trong Postgres
2. Nếu đúng: transaction — set `EXPIRED`, `qty_avail += quantity`, `qty_reserved -= quantity`
3. `ZREM reservation_expiry {reservation_id}` (dù xử lý hay không, để dọn sạch)
4. Publish event `reservation.expired` lên Kafka cho order_service biết

### 6.2 Cron fallback (lưới an toàn, phòng khi Redis worker miss/crash)

Chạy mỗi 5 phút trong `inventory_service`:

```sql
SELECT reservation_id, sku, quantity
FROM reservations
WHERE expires_at < NOW() AND status = 'PENDING';
```

Xử lý y hệt bước 6.1 (mark EXPIRED, cộng trả kho, publish event).
Index trên `expires_at` bắt buộc để query này rẻ ở scale lớn.

### 6.3 Vì sao 2 lớp

| Lớp | Vai trò | Độ trễ |
|---|---|---|
| Redis sorted set | Xử lý chính xác, gần đúng giờ | ~vài giây |
| Cron | Fallback, bắt case Redis worker chết/miss | tối đa ~5 phút |

---

## 7. Roadmap implement (thứ tự nên làm)

1. [ ] Tạo schema `inventory`, `reservations` (inventory_service) và `orders` (order_service)
2. [ ] Implement optimistic locking cho reserve (mục 5)
3. [ ] Implement API 5 endpoint (mục 3) + idempotency key middleware
4. [ ] Implement Redis sorted set worker cho expiry (mục 6.1)
5. [ ] Implement cron fallback (mục 6.2)
6. [ ] Setup Kafka topic `reservation.expired`, `order.paid` + consumer 2 chiều (mục 4.2, 4.3)
7. [ ] (Optional, nếu cần scale) Bucket sharding Redis cho SKU hot — chỉ làm khi có nhu cầu thực tế

---

## 8. Việc KHÔNG làm ở giai đoạn đầu

- Không cần bucket sharding ngay (chỉ cần khi có SKU cực hot / flash sale)
- Không cần Kafka delay-topic pattern (Redis sorted set đã đủ, đơn giản hơn nhiều)
- Không share DB / FOREIGN KEY giữa 2 service
