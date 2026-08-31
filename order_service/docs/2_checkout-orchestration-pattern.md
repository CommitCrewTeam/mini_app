# Note: Orchestration Pattern cho API Checkout (Buy Now Init & tương tự)

## Nguyên tắc cốt lõi
> **FE chỉ gọi 1 endpoint cho 1 màn hình/1 use case.** Việc gộp dữ liệu từ nhiều service là trách nhiệm của backend (orchestration), KHÔNG đẩy việc gọi nhiều service ra client.

Áp dụng cho mọi API kiểu "khởi tạo màn hình" (`/checkout/buy-now/init`, `/checkout/quote`...) — không riêng gì Buy Now.

---

## ❌ Anti-pattern: FE tự gọi nhiều service

```
FE → product_service   (giá, tên, ảnh)
FE → inventory_service (tồn kho)
FE → promotion_service (voucher gợi ý)
FE → shipping_service  (phương thức ship mặc định)
FE → user_service      (địa chỉ mặc định)
   → tự gộp lại ở client để render
```

**Vấn đề:**
1. Nhiều round-trip network từ mobile/browser thay vì 1 → chậm, đặc biệt mobile mạng yếu.
2. Business logic (tính subtotal, chọn voucher default...) bị lộ ra FE, phải viết lại trên iOS/Android/Web, dễ lệch nhau.
3. Đổi cấu trúc service nội bộ sau này → phải sửa cả app đã ngoài store, không kiểm soát được rollout.
4. Bảo mật/rate-limit khó quản lý — mỗi service phải tự public API + tự lo auth.

---

## ✅ Đúng: Backend orchestrate, gộp trả 1 lần

```
FE → order_service (POST /checkout/buy-now/init)
        │
        ├─ gọi product_service   (giá, tên, ảnh)
        ├─ gọi inventory_service (check tồn kho)
        ├─ gọi promotion_service (voucher preview)
        ├─ gọi shipping_service  (default option)
        └─ gọi user_service      (địa chỉ mặc định)
        ↓
     gộp lại 1 response duy nhất trả về FE
```

### Code mẫu (Application Service orchestrate)

```java
@Service
public class BuyNowCheckoutService implements InitBuyNowCheckoutUseCase {

    private final ProductPort productPort;
    private final InventoryPort inventoryPort;
    private final PromotionPort promotionPort;
    private final ShippingPort shippingPort;
    private final CustomerAddressPort customerAddressPort;
    private final QuoteStorePort quoteStorePort;

    @Override
    public CheckoutInitResult init(InitBuyNowCommand command) {
        // Gọi song song những call độc lập để giảm tổng latency
        CompletableFuture<ProductInfo> productFuture =
                CompletableFuture.supplyAsync(() -> productPort.getProduct(command.variantId()));
        CompletableFuture<Address> addressFuture =
                CompletableFuture.supplyAsync(() -> customerAddressPort.getDefault(command.customerId()));

        ProductInfo product = productFuture.join();
        Address address = addressFuture.join();

        inventoryPort.checkAvailable(command.variantId(), command.quantity()); // throw nếu hết hàng

        ShippingOption shipping = shippingPort.getDefaultOption(address, product);
        PromotionResult promo = promotionPort.calculatePreview(...); // chưa chọn voucher nào, mặc định

        long subtotal = product.unitPrice() * command.quantity();
        long total = subtotal + shipping.fee() - promo.discountAmount();

        String quoteId = UUID.randomUUID().toString();
        quoteStorePort.save(quoteId, new StoredQuote(...), Duration.ofMinutes(15));

        return new CheckoutInitResult(quoteId, product, address, shipping, promo, subtotal, total);
    }
}
```

---

## Data nào cần gọi đồng bộ (real-time), data nào có thể cache

| Data | Cách lấy khuyến nghị | Lý do |
|---|---|---|
| Giá sản phẩm, tên, ảnh | Gọi đồng bộ `product_service`; nếu có read-model riêng (đồng bộ qua event `ProductPriceChanged`) thì đọc local | Giá cần chính xác tại thời điểm checkout, nhưng có thể cache ngắn hạn nếu latency cao |
| Tồn kho | Gọi đồng bộ `inventory_service`, **KHÔNG cache** | Sai lệch dẫn tới overselling, không chấp nhận được |
| Địa chỉ mặc định user | Gọi đồng bộ `user_service`/`address_service` | Ít thay đổi, latency không đáng ngại |
| Voucher preview | Gọi đồng bộ `promotion_service` | Cần đúng điều kiện áp dụng hiện tại |
| Phương thức ship + phí | Gọi đồng bộ `shipping_service` | Phí phụ thuộc carrier real-time |

→ Bước "hiển thị giá cho user quyết định mua" hầu hết cần đồng bộ, không chấp nhận data cũ/stale — khác với trang browsing (danh sách sản phẩm) có thể cache/CQRS thoải mái hơn.

---

## Có cần tách riêng 1 lớp BFF (Backend For Frontend) không?

| Phương án | Khi nào phù hợp |
|---|---|
| `order_service` tự đứng ra gọi hộ (như code trên) | Team nhỏ, ít loại client (web + mobile dùng chung API), độ phức tạp gộp vừa phải |
| Tách riêng `checkout_bff_service` | Nhiều loại client cần response shape khác nhau (mobile cần ít field hơn để giảm băng thông), hoặc luồng checkout đủ phức tạp khiến `order_service` phình to, lẫn lộn giữa "orchestrate hiển thị" và "orchestrate nghiệp vụ tạo Order" |

**Khuyến nghị hiện tại (quy mô mini app, đang xây từ đầu):** để `order_service` tự đảm nhiệm việc gộp — chưa cần tách BFF riêng, tránh over-engineering. Chỉ tách BFF khi `order_service` bắt đầu phình to vì lẫn logic hiển thị với logic nghiệp vụ, hoặc có thêm nhiều loại client khác nhau (app riêng cho Shop, admin dashboard...) cần response khác biệt đáng kể.
