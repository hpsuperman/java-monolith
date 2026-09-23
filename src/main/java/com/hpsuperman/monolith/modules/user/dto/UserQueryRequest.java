package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.common.dto.PageQuery;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "用户分页查询条件")
public class UserQueryRequest extends PageQuery {
    private static final long serialVersionUID = 1L;

    @Schema(description = "登录名，模糊匹配")
    private String username;

    @Schema(description = "昵称，模糊匹配")
    private String nickname;

    @Schema(description = "状态：1=启用，0=禁用", allowableValues = {"0", "1"})
    private EnabledStatus status;
}
