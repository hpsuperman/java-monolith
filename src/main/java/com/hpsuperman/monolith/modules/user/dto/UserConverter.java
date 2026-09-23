package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.modules.user.entity.User;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class UserConverter {
    private UserConverter() {
    }

    public static UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setRoles(parseRoles(user.getRoles()));
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    public static List<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    public static String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }

        return roles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    public static String canonicalRoles(String roles) {
        return parseRoles(roles).stream()
                .map(UserConverter::stripRolePrefix)
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    public static boolean hasRole(String roles, String role) {
        return parseRoles(roles).stream().anyMatch(r -> stripRolePrefix(r).equals(role));
    }

    private static String stripRolePrefix(String role) {
        return role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role;
    }
}
