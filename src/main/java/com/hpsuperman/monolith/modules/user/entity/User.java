package com.hpsuperman.monolith.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hpsuperman.monolith.common.entity.BaseEntity;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "password")
@TableName("sys_user")
public class User extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String username;

    @JsonIgnore
    private String password;

    private String nickname;

    private String email;

    private String phone;

    private EnabledStatus status;

    private String roles;
}
