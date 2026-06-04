package com.iam.identity.dto.request;

import java.util.Optional;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdatePersonalInfoRequest {

    private Optional<String>      displayName;   // không cho phép null khi present
    private Optional<String>      avatarUrl;      // null → xóa avatar
    private Optional<String>      emailPersonal;  // phải đúng format email khi present
    private Optional<AddressInfo> address;        // null → không cập nhật địa chỉ

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AddressInfo {
        private String type;          // PERMANENT | TEMPORARY | BIRTH
        private Long   wardCode;
        private Long   provinceCode;
        private String detail;
    }
}
