package com.hpsuperman.monolith.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hpsuperman.monolith.common.entity.BaseEntity;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "password")
public class User extends BaseEntity {
    private String phone;
    private String password;
    private String nickname;
    private String email;
    private String roles;
    private EnabledStatus status;

    @TableLogic(value = "1", delval = "0")
    private Integer deleted;
}
