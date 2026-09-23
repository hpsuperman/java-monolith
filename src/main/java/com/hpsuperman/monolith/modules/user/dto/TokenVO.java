package com.hpsuperman.monolith.modules.user.dto;

import lombok.Data;

@Data
public class TokenVO {
    private String accessToken;
    private String refreshToken;
}
