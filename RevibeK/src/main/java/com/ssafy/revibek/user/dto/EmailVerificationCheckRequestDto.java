package com.ssafy.revibek.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationCheckRequestDto {

    @NotBlank(message = "email은 필수입니다.")
    @Email(message = "올바른 email 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "code는 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "code는 6자리 숫자여야 합니다.")
    private String code;
}
