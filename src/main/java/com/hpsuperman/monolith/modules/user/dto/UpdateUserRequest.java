package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class UpdateUserRequest {
    @Size(max = 100, message = "昵称最长 100 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 个字符")
    private String email;

    @Size(max = 500, message = "头像地址最长 500 个字符")
    private String avatar;

    private List<Role> roles;

    private EnabledStatus status;
}
