# Kịch bản Demo — IAM Banking System

> **Mục tiêu:** Thầy giáo hiểu được hệ thống IAM kiểm soát quyền truy cập vào các ứng dụng nội bộ ra sao — từ khi nhân viên vào, xin thêm quyền, thăng chức, đến khi nghỉ việc.
> **Thời gian ước tính:** 25–30 phút
> **Nguyên tắc:** Ẩn kỹ thuật, chỉ show chức năng và kết quả trực quan. Mọi thứ xảy ra real-time trước mắt thầy.

---

## Nhân vật demo

| Nhân vật | Tài khoản | Vai trò |
|---|---|---|
| **ADMIN** | `admin_system_dev` | Quản trị viên IAM |
| **Lê Thị Hoa** | `hoa.le` | CAB — IT Manager, người duyệt quyền |
| **Trần Văn Minh** | *(tạo live)* | Nhân viên mới, Chuyên viên cấp 1 (CV1) |
| **Nguyễn Văn Hùng** | *(đã tạo sẵn)* | CV sẵn sàng để demo luân chuyển → Chuyên gia |

---

## Ứng dụng trong demo

| Ứng dụng | URL | Vai trò |
|---|---|---|
| **IAM Portal** | http://localhost:4200 | Giao diện quản trị — ADMIN và CAB làm việc ở đây |
| **GitLab CE** | http://localhost:8929 | Quản lý source code |
| **Log System (Kibana)** | http://localhost:5601 | Xem log real-time toàn hệ thống IAM |
| **Change Management App** | http://localhost:8085 | App nghiệp vụ nội bộ — quản lý change trước go-live |

---

## 1. Mở đầu — Giới thiệu bài toán (3 phút)

**Nói với thầy:**

> *"Trong một ngân hàng, phòng CNTT có nhiều lập trình viên, chuyên viên kỹ thuật làm việc hằng ngày với nhiều hệ thống khác nhau:*
> - *GitLab — quản lý mã nguồn, nơi dev đẩy code hằng ngày*
> - *Kibana — xem log toàn hệ thống, phát hiện sự cố bảo mật*
> - *Change Management App — đề xuất và kiểm soát thay đổi trước khi lên production*
>
> *Bài toán: **Ai được truy cập vào đâu? Với quyền gì?** Khi nhân viên mới vào — phải có đúng quyền ngay. Khi cần thêm quyền đặc biệt — phải qua người có thẩm quyền phê duyệt. Khi thăng chức — bộ quyền phải tự động cập nhật. Khi nghỉ việc — toàn bộ quyền phải bị thu hồi ngay, không sót.*
>
> *Đây là hệ thống IAM giải quyết bài toán đó."*

---

## 2. Scene 1 — Nhân viên mới, quyền được cấp tự động (6 phút)

**Câu chuyện:** Phòng CNTT hôm nay có nhân viên mới là Trần Văn Minh, vị trí **Chuyên viên cấp 1 (CV1)**. ADMIN tạo tài khoản trong 1 phút — Minh có thể làm việc ngay.

### Bước 1 — ADMIN tạo tài khoản

1. Mở **http://localhost:4200** → đăng nhập `admin_system_dev`
2. **Quản lý User → Thông tin User** → nhấn nút **[+]** góc phải
3. Điền form:
   - Họ tên: `Trần Văn Minh`
   - Email cá nhân: *(email nhận thông báo)*
   - Phòng ban: `Phòng Phát triển CNTT`
   - Vị trí: **`CV1 — Chuyên viên cấp 1`**
   - Vai trò: `STAFF`
4. Nhấn **"Tạo tài khoản"** → thông báo thành công
5. Ghi lại username tự sinh (VD: `minhTV`)

**Điểm nhấn:**
> *"Hệ thống tự sinh username và mật khẩu tạm — ADMIN không cần đặt thủ công."*

### Bước 2 — Kiểm tra quyền mặc định đã được cấp

1. Click vào **Trần Văn Minh** → trang chi tiết
2. Cuộn xuống **"Quyền hiện tại"** → Tab **Quyền ứng dụng**

Minh đã có ngay:

| Ứng dụng | Nguồn | Ý nghĩa |
|---|---|---|
| **GitLab** | `SYSTEM` | Chuyên viên cần push/review code hằng ngày |
| **IAM Portal** | `SYSTEM` | Tự phục vụ: xem hồ sơ, đổi mật khẩu, xin quyền |

**Chưa có:**
- `log-app-service` (Kibana) — iam-system-logs chứa dữ liệu bảo mật nhạy cảm hơn, phải xin riêng
- `change-mgmt` (Change App) — tạo change yêu cầu thẩm quyền, CV1 chưa có

**Điểm nhấn:**
> *"Ngay khi tạo tài khoản, Minh có đúng bộ quyền cho vị trí CV1 — không cần ADMIN cấp từng cái một. Quyền nào nhạy cảm hơn thì phải xin riêng."*

### Bước 3 — Show email chào mừng

- Mở Gmail → show email gửi đến email cá nhân của Minh
- Nội dung: tên đăng nhập, mật khẩu tạm

### Bước 4 — Minh đăng nhập GitLab thành công

1. Mở tab mới → **http://localhost:8929** (GitLab)
2. Nhập tài khoản IAM của Minh → **Đăng nhập thành công** ✓

**Điểm nhấn:**
> *"1 tài khoản IAM — dùng được trên GitLab ngay. Không cần tạo tài khoản riêng trên GitLab."*

---

## 3. Scene 2 — Xin thêm quyền xem log, CAB phê duyệt (8 phút)

**Câu chuyện:** Minh phát hiện có sự cố kết nối, cần vào Kibana để đọc log hệ thống IAM. Nhưng quyền đọc log IAM nhạy cảm — phải được IT Manager phê duyệt.

### Bước 1 — Minh thử vào Kibana (thất bại)

1. Mở tab mới → **http://localhost:5601** (Kibana)
2. Nhập tài khoản IAM của Minh → **"Invalid credentials"** ✗

**Điểm nhấn:**
> *"Kibana dùng cùng hệ thống xác thực IAM. Minh chưa được cấp quyền xem log IAM nên không đăng nhập được."*

### Bước 2 — Minh tạo yêu cầu phân quyền

1. Mở IAM Portal với tài khoản `minhTV`
2. **Quản lý User → Phân quyền** → Tab **Danh sách yêu cầu** → nhấn **[+]**
3. Điền form:
   - **Người duyệt:** `Lê Thị Hoa` (IT Manager — CAB)
   - **Lý do:** `Cần theo dõi log IAM để điều tra sự cố kết nối đang xảy ra`
   - **Ứng dụng xin thêm:** `log-app-service`
   - **Tài nguyên:** `iam-system-logs — Hành động: view`
4. **"Lưu nháp"** → status `DRAFT` → kiểm tra lại → **"Gửi yêu cầu"** → status `OFFICIAL`

**Điểm nhấn:**
> *"Có 2 bước: lưu nháp để kiểm tra, rồi mới gửi chính thức. Sau khi gửi, hệ thống tự thông báo đến người duyệt."*

### Bước 3 — CAB nhận email, đăng nhập duyệt

1. Show Gmail → email gửi đến `Lê Thị Hoa`
2. Mở **tab ẩn danh** → **http://localhost:4200** → đăng nhập `hoa.le`
3. **Phân quyền → Tab "Duyệt yêu cầu"** → click vào yêu cầu của Minh
4. Đọc: ai xin, lý do, tài nguyên cần cấp
5. Nhấn **"Duyệt"** → ghi chú: `Xác nhận sự cố, cho phép xem log IAM` → Xác nhận

### Bước 4 — Quyền được cấp ngay, Kibana hoạt động

1. Quay lại tab ADMIN → trang detail Minh → **"Quyền hiện tại"**
2. `log-app-service` đã xuất hiện — nguồn **`REQUEST`** *(phân biệt với `SYSTEM`)*
3. Quay lại Kibana → đăng nhập lại bằng tài khoản Minh → **Đăng nhập thành công** ✓
4. Vào **Discover → Data View: `logs-iam`** → **Log IAM chạy real-time**

**Điểm nhấn kép:**
> *"Vừa được duyệt — vào Kibana được ngay. Không restart, không chờ đợi."*
>
> *"Đây là log IAM đang chạy real-time: ai đăng nhập, token nào được cấp, sự kiện bảo mật nào xảy ra. Dữ liệu nhạy cảm — nên cần có người phê duyệt trước khi cấp."*

---

## 4. Scene 3 — Luân chuyển / Thăng chức: CV → Chuyên gia (6 phút)

**Câu chuyện:** Nguyễn Văn Hùng, Chuyên viên kỹ thuật, sau thời gian làm việc được thăng lên **Chuyên gia**. Vị trí mới có toàn quyền vào tất cả hệ thống — hệ thống IAM tự động cập nhật, không cần ADMIN can thiệp từng bước.

> *(Hùng đã được tạo sẵn ở vị trí CV trước khi demo để tiết kiệm thời gian)*

### Bước 1 — Xem trạng thái quyền của Hùng trước khi thăng chức

1. Vào trang detail **Nguyễn Văn Hùng** → **"Quyền hiện tại"**
2. Hùng đang có: GitLab + IAM Portal (giống CV1 tiêu chuẩn)
3. Chưa có: log-app-service, change-mgmt

### Bước 2 — ADMIN thực hiện luân chuyển

1. **Quản lý User → Biến động nhân sự → Tab Luân chuyển**
2. Tìm **Nguyễn Văn Hùng** → mở form luân chuyển
3. Cập nhật:
   - Vị trí mới: **`Chuyên gia`**
   - Vai trò: giữ nguyên `STAFF`
4. Xác nhận

### Bước 3 — So sánh quyền trước / sau

1. Vào lại trang detail Hùng → **"Quyền hiện tại"**

| | Trước (CV) | Sau (Chuyên gia) |
|---|---|---|
| GitLab | ✅ SYSTEM | ✅ SYSTEM |
| IAM Portal | ✅ SYSTEM | ✅ SYSTEM |
| **log-app-service** | ❌ | ✅ **SYSTEM — tự động** |
| **change-mgmt** | ❌ | ✅ **SYSTEM — tự động** |

2. Mở Kibana → đăng nhập bằng tài khoản Hùng → **Vào được** ✓
3. Mở Change App → đăng nhập bằng tài khoản Hùng → **Vào được** ✓

**Điểm nhấn:**
> *"Chỉ thay đổi vị trí — toàn bộ quyền được cập nhật tự động. Hùng giờ vào được tất cả hệ thống mà không cần xin từng cái."*
>
> *"Hệ thống biết Chuyên gia cần làm gì và tự cấp đúng quyền theo cấu hình. ADMIN không làm gì thêm sau khi ấn Xác nhận."*

---

## 5. Scene 4 — Nghỉ việc: Thu hồi toàn bộ quyền ngay lập tức (4 phút)

**Câu chuyện:** Nguyễn Văn Hùng — vừa được thăng Chuyên gia với full quyền vào tất cả hệ thống — quyết định nghỉ việc. Hệ thống IAM phải đảm bảo toàn bộ quyền bị thu hồi ngay lập tức, không có cửa sổ nguy hiểm.

### Bước 1 — Hùng đang vào được mọi thứ (trạng thái trước)

*(Vừa confirm ở Scene 3 — Kibana ✓, Change App ✓, GitLab ✓)*

### Bước 2 — ADMIN thực hiện Offboard

1. **Quản lý User → Biến động nhân sự → Tab Thôi việc**
2. Tìm **Nguyễn Văn Hùng** → mở form thôi việc
3. Nhập ngày thôi việc → Xác nhận → **Hùng được đánh dấu INACTIVE**

### Bước 3 — Kiểm tra tức thì

1. Kibana → đăng xuất Hùng → đăng nhập lại → **"Invalid credentials"** ✗
2. GitLab → đăng xuất Hùng → đăng nhập lại → **Đăng nhập thất bại** ✗
3. Change App → đăng xuất Hùng → đăng nhập lại → **Không vào được** ✗

**Điểm nhấn chốt:**
> *"Ngay khi ADMIN ấn xác nhận thôi việc — tài khoản Hùng không vào được bất kỳ hệ thống nào. GitLab, Kibana, Change App — tất cả đều từ chối ngay lập tức. Không có cửa sổ nguy hiểm, không cần admin vào từng hệ thống để xóa tay."*
>
> *"Đây là giá trị cốt lõi của IAM tập trung: 1 điểm kiểm soát, tất cả hệ thống phụ thuộc vào."*

---

## 6. Tổng kết (2 phút)

**Nói với thầy:**

> *"Qua 4 scene vừa rồi, chúng ta thấy hệ thống IAM giải quyết được toàn bộ vòng đời nhân viên:*
>
> 1. **Vào làm** → quyền mặc định theo vị trí được cấp tự động ngay khi tạo tài khoản
> 2. **Cần thêm quyền** → có quy trình phê duyệt có người chịu trách nhiệm, có lý do, có audit trail
> 3. **Thăng chức** → bộ quyền tự động reset theo vị trí mới, không cần ADMIN can thiệp từng hệ thống
> 4. **Nghỉ việc** → toàn bộ quyền bị thu hồi ngay lập tức trên tất cả hệ thống
>
> *Điểm khác biệt: kiểm soát **tập trung**. Thay vì admin vào từng hệ thống (GitLab, Kibana, Change App...) để cấp và thu hồi quyền riêng lẻ — IAM là 1 điểm điều phối, tất cả hệ thống phụ thuộc vào. Nhanh hơn, ít sai sót hơn, và có đầy đủ dấu vết kiểm toán."*

---

## Checklist chuẩn bị trước khi demo

### Dịch vụ phải đang chạy

- [ ] IAM Portal: **http://localhost:4200**
- [ ] iam-auth-service: port 8888
- [ ] iam-identity-service: port 8081
- [ ] iam-app-service: port 8082
- [ ] iam-gateway: port 8080
- [ ] iam-notify-service: port 8083
- [ ] ldap-server: port 10389
- [ ] GitLab CE: **http://localhost:8929** (LDAP trỏ về ldap-server)
- [ ] Kibana: **http://localhost:5601** (ES + Fluent Bit đang chạy)
- [ ] Change App: **http://localhost:8085**
- [ ] Kafka + Redis (Docker)

### Seed data default permissions (kiểm tra trong IAM Portal → Config → Quyền mặc định)

**CV1 / STAFF phải có:**

| Loại | App / Resource | Action |
|---|---|---|
| App | `gitlab-server` | — |
| App | `iam-service` | — |
| Resource | `iam-service / user-permission` | read, create |

**Chuyên gia / STAFF phải có thêm:**

| Loại | App / Resource | Action |
|---|---|---|
| App | `log-app-service` | — |
| App | `change-mgmt` | — |
| Resource | `log-app-service / iam-system-logs` | view |
| Resource | `change-mgmt / change-request` | read, create, update |

> Nếu chưa có: vào **Config → Quyền mặc định → Tạo mới** cho từng dòng.

### Tài khoản cần chuẩn bị

- [ ] `admin_system_dev` đăng nhập được IAM Portal (kiểm tra BCrypt hash)
- [ ] `hoa.le` — CAB, có email để nhận notification
- [ ] **Nguyễn Văn Hùng** — đã tạo sẵn ở vị trí CV, đăng nhập được GitLab *(test trước)*
- [ ] Xóa tài khoản test cũ tên Trần Văn Minh nếu có (tránh trùng username)

### Kibana phải có log sẵn

- [ ] Vào Kibana → Discover → Data View `logs-iam` → phải thấy log trong 15 phút gần nhất
- [ ] Nếu trống: gọi vài API qua IAM Portal để tạo log (đăng nhập, xem danh sách user...)
- [ ] Data View `logs-iam` đã được tạo sẵn trong Kibana

### ES LDAP cache

- [ ] Cache đã tắt (`cache.ttl: 0` trong `elasticsearch.yml`) — grant/revoke có hiệu lực real-time
- [ ] ES container đã restart để load config mới (`docker restart elasticsearch`)

### Gmail SMTP

- [ ] iam-notify-service đang gửi được email (test: tạo user test → check Gmail)
- [ ] App Password `vdpicjmpwiqnzupz` đang active (nếu không → tạo App Password mới trong Google Account)

### Mở sẵn trước khi demo bắt đầu

- [ ] Tab 1: IAM Portal — đã đăng nhập `admin_system_dev`
- [ ] Tab 2: GitLab — trang login
- [ ] Tab 3: Kibana — trang login
- [ ] Tab 4: Change App — trang login
- [ ] Tab 5: Gmail — inbox của Hoa (CAB)
- [ ] Tab ẩn danh: sẵn sàng để đăng nhập Hoa (CAB)
