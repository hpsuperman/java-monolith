package com.hpsuperman.monolith.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 3, max = 32, message = "密码长度需在 3-32 位之间")
    private String password;


    private String nickname;
    private String email;
}
