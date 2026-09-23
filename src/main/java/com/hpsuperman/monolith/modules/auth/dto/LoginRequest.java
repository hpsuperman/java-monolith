package com.hpsuperman.monolith.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "登录请求")
public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名过长")
    @Schema(description = "登录名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码过长")
    @Schema(description = "密码", example = "admin123")
    private String password;
}
