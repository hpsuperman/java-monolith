package com.hpsuperman.monolith.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserVO {
    private Long id;
    private String phone;
    private String nickname;
    private String email;
    private String avatar;
    private List<String> roles;
    private LocalDateTime createTime;
}
