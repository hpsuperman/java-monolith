package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CreateUserRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 3, max = 32, message = "密码长度需在 3-32 位之间")
    private String password;

    @Size(max = 100, message = "昵称最长 100 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 个字符")
    private String email;

    private List<Role> roles;
    private EnabledStatus status;
}
