package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "登录名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：1=启用，0=禁用", allowableValues = {"0", "1"})
    private EnabledStatus status;

    @Schema(description = "角色码列表")
    private List<String> roles;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
