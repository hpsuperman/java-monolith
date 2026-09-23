package com.hpsuperman.monolith.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum EnabledStatus {
    DISABLED(0),

    ENABLED(1);

    @EnumValue
    @JsonValue
    private final int value;

    EnabledStatus(int value) {
        this.value = value;
    }

    @JsonCreator
    public static EnabledStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(status -> status.value == value)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("未知的状态值：" + value + "，只能是 0 或 1"));
    }
}
