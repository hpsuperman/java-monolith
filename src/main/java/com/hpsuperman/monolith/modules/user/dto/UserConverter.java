package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.common.enums.Role;
import com.hpsuperman.monolith.modules.user.entity.User;

public final class UserConverter {
    private UserConverter() {
    }

    public static UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setRoles(Role.split(user.getRoles()));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    public static AdminUserVO toAdminVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setRoles(Role.split(user.getRoles()));
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
