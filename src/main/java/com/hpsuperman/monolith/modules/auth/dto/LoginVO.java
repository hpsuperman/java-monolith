package com.hpsuperman.monolith.modules.auth.dto;

import com.hpsuperman.monolith.modules.user.dto.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(description = "登录/刷新响应")
public class LoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "访问令牌，放到 Authorization: Bearer <token> 里")
    private String accessToken;

    @Schema(description = "刷新令牌，accessToken 过期后用它换新的")
    private String refreshToken;

    @Schema(description = "令牌类型，固定为 Bearer")
    private String tokenType;

    @Schema(description = "accessToken 剩余有效秒数")
    private long expiresIn;

    @Schema(description = "当前登录用户信息")
    private UserVO user;
}
