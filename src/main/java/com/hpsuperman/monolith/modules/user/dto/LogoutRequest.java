package com.hpsuperman.monolith.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
