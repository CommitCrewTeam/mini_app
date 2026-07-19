# Command Notes

## Docker Compose

Chạy toàn bộ service trong `docker-compose.yml`:

```bash
docker compose up -d
```

Dừng và xoá container/network của compose:

```bash
docker compose down
```

Clean rồi chạy lại từ đầu:

```bash
docker compose down --remove-orphans
docker compose up -d
```

Kiểm tra service đang chạy:

```bash
docker compose ps
```

Xem log:

```bash
docker compose logs -f
```
