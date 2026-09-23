package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "更新用户请求")
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 32, message = "昵称最长 32 位")
    @Schema(description = "昵称")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱最长 128 位")
    @Schema(description = "邮箱")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：1=启用，0=禁用；不传表示不修改", allowableValues = {"0", "1"})
    private EnabledStatus status;

    @Schema(description = "角色码列表，传了就整体覆盖；传空数组表示清空所有角色")
    private List<String> roles;
}
