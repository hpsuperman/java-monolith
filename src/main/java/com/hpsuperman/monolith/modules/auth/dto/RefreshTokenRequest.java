package com.hpsuperman.monolith.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "refreshToken 不能为空")
    @Schema(description = "登录时返回的 refreshToken")
    private String refreshToken;
}
