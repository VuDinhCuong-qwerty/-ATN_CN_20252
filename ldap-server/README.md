# ldap-server

Thư mục này dành cho triển khai LDAP Server tích hợp với hệ thống IAM Banking.

## Mục tiêu

- Tích hợp xác thực người dùng qua LDAP/Active Directory
- Đồng bộ thông tin nhân viên từ directory server
- Hỗ trợ single sign-on (SSO) qua LDAP bind

## Hướng triển khai

- **OpenLDAP** hoặc **Apache Directory Server** làm LDAP server
- Spring Security LDAP trong `iam-auth-service` để xác thực
- Định kỳ đồng bộ `uid`, `cn`, `mail`, `departmentNumber` từ LDAP về `AUTH_USER` (Oracle)

## Chưa triển khai
