package com.dat.backend_v2_1.dto.Security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Set;

@Data
public class UserReq {
    @NotBlank(message = "Phone number must not be blank")
    @Pattern(regexp = "^0(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])\\d{7}$",
            message = "Phone number must be a normalized Vietnamese mobile number")
    private String phoneNumber;

    @NotEmpty(message = "Role codes must not be empty")
    private Set<String> roleCodes;
}
