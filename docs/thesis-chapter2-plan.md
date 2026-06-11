# Kế hoạch viết Chương 2 — Khảo sát và Phân tích Yêu cầu

## Định hướng tổng thể

Bài toán cốt lõi là **quản lý phân quyền** với 7 luồng nghiệp vụ chính.
CRUD user và cấu hình hệ thống chỉ đề cập như điều kiện kỹ thuật — không phân rã sâu.

---

## 2.1 Khảo sát hiện trạng

- So sánh 3–4 giải pháp IAM phổ biến: Keycloak, Microsoft AD/Azure AD, Okta
- Trục so sánh: OAuth2/OIDC, approval workflow, lifecycle management, LDAP gateway, mức độ tùy chỉnh, chi phí
- Kết luận: các giải pháp thiếu approval workflow tích hợp + tự động hóa lifecycle → khoảng trống đề tài lấp vào
- Nhu cầu 3 vai: ADMIN, CAB, STAFF (phỏng vấn mô phỏng)

---

## 2.2 Tổng quan chức năng

### 2.2.1 Biểu đồ use case tổng quát

**Actors:**
- ADMIN — quản trị viên hệ thống, toàn quyền
- CAB (Change Advisory Board) — hội đồng phê duyệt quyền (IT Manager, CISO)
- STAFF — nhân viên IT, người thụ hưởng quyền
- System — tác nhân tự động (cấp quyền mặc định, gửi email)

**7 UC chính** (mỗi cái có mục phân rã riêng):
1. Xin & phê duyệt cấp quyền
2. Tạo người dùng mới
3. Tiếp nhận nhân viên (Onboard)
4. Thôi việc (Offboard)
5. Nghỉ phép (Leave)
6. Trở lại (Return)
7. Luân chuyển công tác (Transfer)

**3 UC phụ trợ** (chỉ xuất hiện ở sơ đồ tổng quát, 1–2 câu giải thích, không phân rã):
- Quản lý thông tin người dùng — điều kiện dữ liệu đầu vào cho các luồng phân quyền
- Quản lý cấu hình hệ thống (ứng dụng, tài nguyên, quyền mặc định) — bộ quy tắc để hệ thống biết cấp quyền gì
- Xác thực & Đăng nhập — điều kiện kỹ thuật để truy cập hệ thống

---

### 2.2.2 Phân rã: Xin & phê duyệt cấp quyền ← ĐẶT ĐẦU TIÊN

- Tạo yêu cầu phân quyền (STAFF)
- Gửi yêu cầu chính thức (STAFF)
- Xem danh sách yêu cầu chờ duyệt (CAB)
- Phê duyệt yêu cầu → System tự động cấp quyền (CAB)
- Từ chối yêu cầu (CAB)
- Hủy yêu cầu (STAFF)

### 2.2.3 Phân rã: Tạo người dùng mới

- Nhập thông tin hồ sơ + chọn vai trò & vị trí (ADMIN)
- Hệ thống tự động cấp quyền mặc định theo (vai trò × vị trí) (System)
- Hệ thống gửi email thông tin tài khoản (System)

### 2.2.4 Phân rã: Tiếp nhận nhân viên (Onboard)

- Kích hoạt lại tài khoản (ADMIN)
- Gán vai trò & vị trí mới (ADMIN)
- Hệ thống tự động cấp quyền mặc định (System)

### 2.2.5 Phân rã: Thôi việc (Offboard)

- Vô hiệu hóa tài khoản (ADMIN)
- Hệ thống thu hồi toàn bộ vai trò và quyền (System)

### 2.2.6 Phân rã: Nghỉ phép (Leave)

- ADMIN thiết lập kỳ nghỉ phép (thời gian từ–đến)
- Hệ thống tạm hoãn quyền trong thời gian nghỉ (System)

### 2.2.7 Phân rã: Trở lại (Return)

- ADMIN xác nhận nhân viên trở lại
- Hệ thống khôi phục quyền về trạng thái trước nghỉ phép (System)

### 2.2.8 Phân rã: Luân chuyển công tác (Transfer)

- ADMIN cập nhật vai trò & vị trí mới
- Hệ thống thu hồi toàn bộ quyền cũ (System)
- Hệ thống cấp quyền mặc định theo vai trò & vị trí mới (System)

---

### 2.2.9 Quy trình nghiệp vụ (Activity Diagrams)

Vẽ 7 activity diagram theo **luồng giao diện** (không phải luồng code), theo đúng thứ tự 7 luồng trên.

Mỗi diagram thể hiện:
- Ai thao tác gì trên giao diện
- Điều kiện rẽ nhánh (VD: CAB duyệt / từ chối)
- Hành động tự động của System sau khi user submit

**Thứ tự vẽ:**
1. Luồng xin & phê duyệt cấp quyền
2. Luồng tạo người dùng mới
3. Luồng onboard
4. Luồng offboard
5. Luồng nghỉ phép
6. Luồng trở lại
7. Luồng luân chuyển

---

## 2.3 Đặc tả chức năng (7 use case)

Đặc tả toàn bộ 7 luồng bằng bảng, đúng thứ tự activity diagram.

| STT | Tên use case | Actors |
|---|---|---|
| 1 | Xin & phê duyệt cấp quyền | STAFF, CAB, System |
| 2 | Tạo người dùng mới | ADMIN, System |
| 3 | Tiếp nhận nhân viên (Onboard) | ADMIN, System |
| 4 | Thôi việc (Offboard) | ADMIN, System |
| 5 | Nghỉ phép (Leave) | ADMIN, System |
| 6 | Trở lại (Return) | ADMIN, System |
| 7 | Luân chuyển công tác (Transfer) | ADMIN, System |

**Mỗi đặc tả gồm:**
- Tên use case
- Tiền điều kiện
- Hậu điều kiện
- Luồng sự kiện chính
- Luồng sự kiện phát sinh (nếu có)

---

## 2.4 Yêu cầu phi chức năng

- Bảo mật: JWT ES256, BCrypt, HMAC sessionToken, authorization code 1-time use
- Hiệu năng: Caffeine cache (auth flow, signing key), Redis (session, code), Kafka 3-broker
- Tích hợp: OAuth2/OIDC chuẩn + LDAP gateway cho ứng dụng bên thứ ba
- Kiểm toán: ghi nhận grantedBy/revokedBy/grantSource cho mọi hành động cấp/thu hồi quyền
- Kỹ thuật: Oracle 21c, microservices Docker Compose, stateless service
