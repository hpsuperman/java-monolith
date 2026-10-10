package com.hpsuperman.monolith.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 100,message = "昵称最长 100 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100,message = "邮箱最长 100 个字符")
    private String email;

    @Size(max = 500,message = "头像地址最长 500 个字符")
    private String avatar;
}
