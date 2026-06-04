package com.iam.notify.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserEmailRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Tra cứu email công việc (AUTH_USER.EMAIL) theo employee code.
     * Email công việc dùng cho thông báo bảo mật (permission request, approval).
     *
     * @return email nếu tìm thấy, null nếu không có hoặc lỗi
     */
    public String findEmailByEmployeeCode(String employeeCode) {
        if (employeeCode == null || employeeCode.isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT u.EMAIL FROM AUTH_USER u " +
                    "JOIN AUTH_USER_PROFILE up ON up.USER_ID = u.ID " +
                    "WHERE up.EMPLOYEE_CODE = ?",
                    String.class, employeeCode);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No user found for employeeCode={}", employeeCode);
            return null;
        } catch (Exception e) {
            log.error("Failed to lookup email for employeeCode={}: {}", employeeCode, e.getMessage());
            return null;
        }
    }
}
