# 🐛 Bug Report: Login thất bại — `PSQLException: No results were returned by the query`

> **Ngày phát hiện:** 10/03/2026
> **Mức độ:** 🔴 Critical — Không thể đăng nhập vào hệ thống
> **Trạng thái:** ✅ Đã fix

---

## 1. 📋 Mô Tả Lỗi

Khi gọi `POST /api/v1/auth/login` với thông tin đúng, server trả về:

```json
{
    "data": null,
    "error": "InternalServerError",
    "message": "org.postgresql.util.PSQLException: No results were returned by the query.",
    "statusCode": 500
}
```

---

## 2. 🔍 Phân Tích Root Cause

### Stack trace chính

```
InternalAuthenticationServiceException:
    org.postgresql.util.PSQLException: No results were returned by the query.
```

Lỗi xảy ra trong **Spring Security authentication pipeline**, cụ thể tại bước `loadUserByUsername()`.

---

### 🔎 Nguyên nhân gốc rễ — `FloatArrayConverter` implement sai

File: `src/main/java/com/dat/backend_v2_1/util/converter/FloatArrayConverter.java`

```java
// ❌ SAI — JDBC không thể map float[] Java → PostgreSQL vector(512)
public class FloatArrayConverter implements AttributeConverter<float[], float[]> {

    @Override
    public float[] convertToDatabaseColumn(float[] attribute) {
        return attribute; // Trả về chính nó — JDBC không hiểu type này
    }

    @Override
    public float[] convertToEntityAttribute(float[] dbData) {
        return dbData; // JDBC trả về PGobject, không phải float[] → ClassCastException
    }
}
```

**Vấn đề:**

- `AttributeConverter<X, Y>` yêu cầu `Y` phải là kiểu JDBC biết cách xử lý (`String`, `Integer`, `byte[]`,...)
- PostgreSQL `vector(512)` là custom type của extension **pgvector**, JDBC driver trả về `PGobject` chứ không phải
  `float[]`
- Khi Hibernate thực thi SELECT query với JOINED inheritance, nó cố đọc column `face_embedding` → JDBC fail →
  `PSQLException`

---

### 📌 Các lỗi phụ đi kèm

#### Lỗi 2: `UserRepository.findByPhoneNumber` trả về sai type

```java
// ❌ SAI — Optional<Object> không map được sang entity User
Optional<Object> findByPhoneNumber(String phoneNumber);
```

Spring Data JPA không thể generate đúng query khi return type là `Object`.

---

#### Lỗi 3: `GlobalException` — `handleGeneric` đặt sai vị trí gây vòng lặp lỗi

```java
// ❌ SAI — handleGeneric đặt TRƯỚC các handler cụ thể
@ExceptionHandler(Exception.class)         // ← bắt hết mọi exception kể cả UserNotFoundException
public ResponseEntity<...> handleGeneric(Exception ex) { ... }

@ExceptionHandler(UserNotFoundException.class) // ← không bao giờ được gọi
public ResponseEntity<...> handleUserNotFound(UserNotFoundException ex) { ... }
```

Hệ quả: `handleGeneric` bắt `HttpMediaTypeNotAcceptableException` → cố serialize response → fail → Spring gọi lại
`handleGeneric` → **vòng lặp vô hạn**.

---

#### Lỗi 4: Thiếu handler cho `BusinessException`

`BusinessException` không có handler riêng → rơi vào `handleGeneric` → luôn trả về 500.

---

## 3. ✅ Giải Pháp

### Fix 1: `User.java` — Đánh dấu `faceEmbedding` là `@Transient`

File: `src/main/java/com/dat/backend_v2_1/domain/Security/User.java`

```java
// ✅ ĐÚNG — Bỏ qua JPA mapping hoàn toàn, tránh JDBC error với pgvector
@Transient
float[] faceEmbedding;
```

> **Lý do chọn `@Transient`:** Dự án chưa có pgvector Java library (`com.pgvector:pgvector`).
> Field `faceEmbedding` chỉ dùng cho chức năng nhận diện khuôn mặt, không liên quan đến login/authentication.
> Khi cần dùng đúng cách, thêm dependency pgvector và implement `UserType` tương ứng.

---

### Fix 2: `UserRepository.java` — Sửa return type

File: `src/main/java/com/dat/backend_v2_1/repository/Security/UserRepository.java`

```java
// ❌ Trước
Optional<Object> findByPhoneNumber(String phoneNumber);

// ✅ Sau
Optional<User> findByPhoneNumber(String phoneNumber);
```

---

### Fix 3: `FloatArrayConverter.java` — Sửa implement đúng signature

File: `src/main/java/com/dat/backend_v2_1/util/converter/FloatArrayConverter.java`

```java
// ✅ ĐÚNG — Convert giữa float[] (Java) ↔ String (JDBC-compatible)
public class FloatArrayConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            sb.append(attribute[i]);
            if (i < attribute.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        String trimmed = dbData.trim();
        // Xóa ký tự bao ngoài: '[...]' hoặc '{...}'
        if (!trimmed.isEmpty() && (trimmed.charAt(0) == '[' || trimmed.charAt(0) == '{'))
            trimmed = trimmed.substring(1);
        if (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            if (last == ']' || last == '}') trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) return new float[0];
        String[] parts = trimmed.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++)
            result[i] = Float.parseFloat(parts[i].trim());
        return result;
    }
}
```

---

### Fix 4: `GlobalException.java` — Sắp xếp lại thứ tự handler

File: `src/main/java/com/dat/backend_v2_1/util/error/GlobalException.java`

**Nguyên tắc:** Handler **cụ thể** phải đặt **trước** handler **chung**.

```
✅ Thứ tự đúng:
1. handleAuthException        → UsernameNotFoundException, BadCredentialsException, ...
2. validationError            → MethodArgumentNotValidException
3. handleUserNotFound         → UserNotFoundException
4. handleBusinessException    → BusinessException              ← thêm mới
5. handleResponseStatus       → ResponseStatusException
6. handleMediaTypeNotAcceptable → HttpMediaTypeNotAcceptableException  ← thêm mới
7. handleGeneric              → Exception.class               ← đặt CUỐI CÙNG
```

---

## 4. 📊 Bảng Tổng Kết

| # | File                       | Vấn đề                                              | Fix                                             |
|---|----------------------------|-----------------------------------------------------|-------------------------------------------------|
| 1 | `User.java`                | `@Convert` với `vector(512)` → JDBC crash           | Đổi thành `@Transient`                          |
| 2 | `UserRepository.java`      | `Optional<Object>` sai type                         | Đổi thành `Optional<User>`                      |
| 3 | `FloatArrayConverter.java` | `AttributeConverter<float[], float[]>` không hợp lệ | Đổi thành `AttributeConverter<float[], String>` |
| 4 | `GlobalException.java`     | `handleGeneric` đặt trước handler cụ thể → vòng lặp | Sắp xếp lại, thêm 2 handler mới                 |

---

## 5. 💡 Ghi Chú & Khuyến Nghị

### Về `faceEmbedding` / pgvector

Nếu muốn dùng pgvector đúng cách trong tương lai:

1. Thêm dependency vào `pom.xml`:

```xml
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.6</version>
</dependency>
```

2. Đăng ký type trong datasource config:

```java
connection.unwrap(PGConnection .class).

addDataType("vector",PGvector .class);
```

3. Dùng `PGvector` type thay vì `float[]` trong entity.

---

### Về `@Builder.Default` trong `@SuperBuilder`

`User.java` có `@Builder.Default` trên field `status` — khi dùng với `@SuperBuilder`, có thể gây conflict trong một số
trường hợp. Nên xem xét khởi tạo default value trong constructor thay thế.

---

*Báo cáo này được tạo tự động dựa trên quá trình debug ngày 10/03/2026.*

