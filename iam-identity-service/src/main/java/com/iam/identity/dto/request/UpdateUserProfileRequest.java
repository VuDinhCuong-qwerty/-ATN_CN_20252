package com.iam.identity.dto.request;

import java.time.LocalDate;
import java.util.Optional;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateUserProfileRequest {

    private Optional<String>    firstName;
    private Optional<String>    lastName;
    private Optional<String>    fullName;
    private Optional<LocalDate> dob;
    private Optional<String>    gender;           // MALE | FEMALE | OTHER
    private Optional<String>    nationality;
    private Optional<String>    ethnic;
    private Optional<String>    religion;
    private Optional<String>    cccd;             // 12 chữ số
    private Optional<LocalDate> cccdIssuedDate;
    private Optional<String>    cccdIssuedPlace;
    private Optional<LocalDate> joinDate;
    private Optional<String>    departmentId;
    private Optional<String>    position;         // → AUTH_POSITION.code
    private Optional<String>    mobile;
}
