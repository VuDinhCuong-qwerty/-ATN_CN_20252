package com.iam.identity.kafka.consumer;

// Topics cần consume:
//   - CREATE-SUCCESS-USER-NOTIFY  → gửi email/push thông báo tạo user thành công
//   - REQUEST-PERMISSION-NOTIFY   → thông báo reviewer có request mới cần duyệt
//   - APPROVE-PERMISSION-NOTIFY   → thông báo requester kết quả approve/reject
//   - REVOKED-PERMISSION-NOTIFY   → thông báo user khi quyền bị thu hồi
//   - USER-CHANGED-PASSWORD       → thông báo user đổi/reset mật khẩu thành công
public class NotifyConsumer {

}
