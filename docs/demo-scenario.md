# Kịch bản Demo — IAM Banking System

> **Mục tiêu:** Thầy giáo hiểu được hệ thống IAM làm gì, kiểm soát quyền truy cập vào các ứng dụng nội bộ ra sao.
> **Thời gian ước tính:** 25–30 phút
> **Nguyên tắc:** Ẩn kỹ thuật, chỉ show chức năng và kết quả trực quan.

---

## 1. Mở đầu — Giới thiệu bài toán (3 phút)

**Nói với thầy:**

> "Trong một ngân hàng, phòng CNTT có hàng chục đến hàng trăm lập trình viên, quản trị viên hệ thống làm việc hằng ngày với nhiều ứng dụng nội bộ khác nhau:
> - **GitLab** — quản lý mã nguồn, nơi lập trình viên đẩy code hằng ngày
> - **Change Management App** — quản lý các thay đổi hệ thống trước khi go-live
> - **Log System (Kibana)** — xem log, monitor hệ thống
>
> Bài toán đặt ra: **Ai được phép truy cập vào ứng dụng nào? Với quyền gì?**
> Khi nhân viên mới vào → phải có đúng quyền ngay. Khi chuyển vị trí → quyền phải được cập nhật. Khi thôi việc → quyền phải bị thu hồi ngay lập tức, không để sót.
>
> Đây là hệ thống IAM — Identity and Access Management — giải quyết bài toán đó."

**Giới thiệu 3 ứng dụng demo:**

| Ứng dụng | URL | Vai trò trong demo |
|---|---|---|
| **IAM Portal** | http://localhost:4200 | Giao diện quản trị — ADMIN và CAB làm việc ở đây |
| **Change Management App** | http://localhost:8085 | App nội bộ demo — nhân viên dùng hằng ngày |
| **GitLab CE** | http://localhost:8929 | Quản lý code — nhân viên cần xin quyền riêng |

**Nhân vật trong demo:**

| Nhân vật | Tài khoản | Vai trò |
|---|---|---|
| **ADMIN** | `admin_system_dev` | Quản trị viên IAM |
| **Lê Thị Hoa** | `hoa.le` | CAB — IT Manager, người duyệt quyền |
| **Trần Văn Minh** | *(sẽ được tạo)* | Nhân viên mới, Kỹ thuật viên IT_L1 |

---

## 2. Scene 1 — Tạo nhân viên mới, quyền được cấp tự động (7 phút)

**Câu chuyện:** Hôm nay phòng CNTT có nhân viên mới là Trần Văn Minh, vị trí Kỹ thuật viên cấp 1. ADMIN cần tạo tài khoản để anh có thể làm việc ngay trong ngày đầu.

### Bước 1 — ADMIN tạo tài khoản

1. Mở trình duyệt, vào **http://localhost:4200**
2. Đăng nhập bằng tài khoản `admin_system_dev`
3. Vào menu **Quản lý User → Thông tin User**
4. Nhấn nút **[+]** (góc phải màn hình)
5. Điền form tạo user:
   - Họ tên: `Trần Văn Minh`
   - Ngày sinh, email công việc, email cá nhân *(điền sẵn)*
   - Phòng ban: `Phòng Phát triển CNTT`
   - Vị trí: `IT_L1 — Kỹ thuật viên cấp 1`
   - Vai trò: `STAFF`
6. Nhấn **"Tạo tài khoản"**
7. Thông báo thành công hiện ra — ghi lại username được sinh tự động (VD: `minhTV`)

**Điểm nhấn khi giới thiệu:**
> "Hệ thống tự sinh username và mật khẩu tạm. ADMIN không cần đặt mật khẩu thủ công."

### Bước 2 — Kiểm tra quyền đã được cấp tự động

1. Trong trang danh sách user, tìm và click vào **Trần Văn Minh**
2. Cuộn xuống section **"Quyền hiện tại"**
3. Show tab **Quyền ứng dụng** → thấy danh sách app đã có quyền (Change Management App, IAM Portal...)
4. Show tab **Quyền tài nguyên** → thấy các quyền cụ thể (read, create...)

**Điểm nhấn:**
> "Ngay sau khi tạo tài khoản, hệ thống **tự động cấp quyền** dựa trên vị trí IT_L1. Không cần ADMIN cấp tay từng quyền một."

### Bước 3 — Nhân viên nhận email (show live hoặc screenshot)

- Mở Gmail — show email chào mừng gửi đến email cá nhân của Minh
- Nội dung: tên đăng nhập, mật khẩu tạm, link đăng nhập

---

## 3. Scene 2 — Nhân viên đăng nhập Change Management App (4 phút)

**Câu chuyện:** Minh nhận email, vào Change App để làm việc.

### Bước 1 — Đăng nhập Change App

1. Mở tab mới, vào **http://localhost:8085**
2. App tự redirect sang trang đăng nhập IAM → nhập `minhTV` / *(mật khẩu tạm)*
3. Đăng nhập thành công → vào được trang chính của Change App

**Điểm nhấn:**
> "Minh chỉ cần 1 tài khoản IAM để đăng nhập vào tất cả ứng dụng. Không cần tạo tài khoản riêng trên từng hệ thống."

### Bước 2 — Thao tác trên Change App

1. Minh tạo một **Change Request** mới (điền tiêu đề, mô tả, ngày thực hiện)
2. Submit → Change Request được tạo thành công
3. Show danh sách Change Request của Minh

---

## 4. Scene 3 — Xin thêm quyền truy cập GitLab (6 phút)

**Câu chuyện:** Dự án mới yêu cầu Minh phải truy cập GitLab để review code. Tuy nhiên theo cấu hình mặc định, IT_L1 chưa có quyền này. Minh cần xin quyền bổ sung thông qua CAB.

### Bước 1 — Minh thử đăng nhập GitLab (thất bại)

1. Mở tab mới, vào **http://localhost:8929** (GitLab)
2. Nhập tài khoản IAM của Minh → **Đăng nhập thất bại**

**Điểm nhấn:**
> "Minh chưa có quyền truy cập GitLab. Để được cấp quyền, cần gửi yêu cầu để CAB xem xét và phê duyệt."

### Bước 2 — Minh tạo yêu cầu phân quyền

1. Vào IAM Portal đang đăng nhập bằng `minhTV`
2. Vào menu **Quản lý User → Phân quyền**
3. Nhấn nút **[+]** (Tạo yêu cầu mới)
4. Điền form:
   - **Người duyệt:** tìm `Lê Thị Hoa` (IT Manager — CAB)
   - **Lý do:** `Tham gia dự án bảo mật Q3/2026, cần review code trên GitLab`
   - **Ứng dụng xin thêm:** chọn `GitLab`
5. Nhấn **"Lưu nháp"** → thấy yêu cầu status `DRAFT`
6. Kiểm tra lại thông tin → nhấn **"Gửi yêu cầu"** → status chuyển sang `OFFICIAL`

**Điểm nhấn:**
> "Yêu cầu có 2 bước: lưu nháp để kiểm tra, rồi mới gửi chính thức. Sau khi gửi, hệ thống tự động thông báo đến người duyệt."

### Bước 3 — CAB nhận email thông báo (show live hoặc screenshot)

- Mở Gmail — show email gửi đến `Lê Thị Hoa`
- Nội dung: ai xin quyền gì, lý do, link để xem xét

---

## 5. Scene 4 — CAB duyệt, quyền được cấp ngay lập tức (5 phút)

**Câu chuyện:** Hoa — IT Manager — nhận được thông báo, vào portal xem xét và phê duyệt.

### Bước 1 — CAB đăng nhập và xem yêu cầu

1. Mở tab ẩn danh (incognito) → vào **http://localhost:4200**
2. Đăng nhập bằng tài khoản `hoa.le` (CAB)
3. Vào **Phân quyền → Tab "Duyệt yêu cầu"**
4. Thấy yêu cầu của Minh — click vào xem chi tiết
5. Đọc: người xin, lý do, ứng dụng cần cấp

### Bước 2 — CAB phê duyệt

1. Nhấn **"Duyệt"**
2. Điền ghi chú: `Xác nhận tham gia dự án bảo mật, hiệu lực đến 31/12/2026`
3. Xác nhận → thành công

### Bước 3 — Kiểm tra quyền của Minh đã được cập nhật

1. Quay lại tab của ADMIN
2. Vào trang chi tiết của **Trần Văn Minh**
3. Section **"Quyền hiện tại"** → tab Quyền ứng dụng
4. Thấy **GitLab** đã xuất hiện với nguồn cấp `REQUEST` *(phân biệt với SYSTEM — tự động)*

**Điểm nhấn:**
> "Ngay sau khi CAB duyệt, quyền được cấp tức thì. Không cần ADMIN can thiệp thêm."

### Bước 4 — Minh đăng nhập GitLab thành công

1. Quay lại tab GitLab đang lỗi → **F5**
2. Nhập lại tài khoản IAM của Minh
3. **Đăng nhập thành công** → vào được GitLab

**Điểm nhấn:**
> "Cùng 1 tài khoản IAM — trước đó không vào được GitLab, sau khi được duyệt thì vào được ngay. GitLab không cần tạo tài khoản riêng."

---

## 6. Scene 5 — Thu hồi quyền (3 phút)

**Câu chuyện:** Dự án kết thúc, Minh không còn cần dùng GitLab nữa. ADMIN thu hồi quyền.

### Bước 1 — ADMIN thu hồi quyền GitLab của Minh

1. Vào trang chi tiết **Trần Văn Minh** (đăng nhập ADMIN)
2. Section **"Quyền hiện tại"** → tìm dòng **GitLab**
3. Nhấn **"Thu hồi"**
4. Xác nhận → thành công

### Bước 2 — Minh không còn vào được GitLab

1. Quay lại tab GitLab của Minh → **F5 hoặc đăng xuất rồi đăng nhập lại**
2. **Đăng nhập thất bại** — quyền đã bị thu hồi

**Điểm nhấn:**
> "Thu hồi quyền có hiệu lực ngay lập tức. Không cần đợi token hết hạn hay admin xử lý trên GitLab."

---

## 7. Scene 6 (Optional) — Luân chuyển công tác (3 phút)

*Bỏ qua nếu hết thời gian*

**Câu chuyện:** Minh được chuyển từ Phòng Phát triển sang Phòng Vận hành, vị trí mới là SYSADMIN. Quyền phải được cập nhật lại theo vị trí mới.

### Bước demo

1. ADMIN vào **Quản lý User → Biến động nhân sự → Tab Luân chuyển**
2. Tìm Trần Văn Minh → mở form luân chuyển
3. Điền: Vị trí mới = `SYSADMIN`, Phòng ban mới = `Phòng Vận hành`
4. Xác nhận

### Kiểm tra kết quả

1. Vào lại trang chi tiết của Minh → section **"Quyền hiện tại"**
2. Thấy: quyền cũ của IT_L1 đã bị thu hồi, quyền mặc định của SYSADMIN đã được cấp mới

**Điểm nhấn:**
> "Toàn bộ quá trình tự động. ADMIN chỉ cần xác nhận luân chuyển — hệ thống tự thu hồi quyền cũ và cấp quyền mới theo vị trí."

---

## 8. Tổng kết demo (2 phút)

**Nói với thầy:**

> "Qua 5 scene vừa rồi, chúng ta thấy hệ thống IAM giải quyết được:
>
> 1. **Tạo nhân viên mới** → quyền được cấp tự động theo vai trò và vị trí, không cần can thiệp thủ công
> 2. **Đăng nhập 1 lần** trên tất cả ứng dụng (IAM Portal, Change App, GitLab)
> 3. **Xin thêm quyền** qua quy trình có người phê duyệt — có lý do, có audit trail
> 4. **Thu hồi quyền** tức thì — GitLab không vào được ngay sau khi bị thu hồi
> 5. **Luân chuyển** — quyền tự động cập nhật, không cần sửa thủ công trên từng hệ thống
>
> Điểm khác biệt so với các giải pháp hiện tại: mọi thứ được kiểm soát **tập trung**, **tự động**, và **có audit trail** — ai cấp quyền gì, cho ai, lúc nào, tại sao."

---

## Checklist chuẩn bị trước khi demo

- [ ] Các service đang chạy: IAM Portal (4200), Change App (8085), GitLab (8929)
- [ ] Tài khoản `admin_system_dev` đăng nhập được IAM Portal
- [ ] Tài khoản `hoa.le` (CAB) đã có sẵn, đăng nhập được
- [ ] GitLab đã cấu hình LDAP trỏ về ldap-server (10389)
- [ ] Email notification đang hoạt động (Gmail SMTP)
- [ ] Seed data: cấu hình quyền mặc định cho IT_L1 và SYSADMIN đã có trong DB
- [ ] Xóa tài khoản test cũ nếu có (tránh username bị trùng khi tạo Trần Văn Minh)
- [ ] Mở sẵn Gmail để show email notification
