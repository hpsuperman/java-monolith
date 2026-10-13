package com.hpsuperman.monolith.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AdminUserVO {
    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private String email;
    private EnabledStatus status;
    private List<String> roles;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
