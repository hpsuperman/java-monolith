package com.hpsuperman.monolith.common.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Role {
    USER,
    ADMIN;

    public static List<String> split(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    public static String join(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.stream().distinct().map(Enum::name).collect(Collectors.joining(","));
    }
}
