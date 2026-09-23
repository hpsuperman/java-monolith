package com.hpsuperman.monolith.common.exception;

import com.hpsuperman.monolith.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }

    public BizException(String message) {
        this(ResultCode.BIZ_ERROR, message, null);
    }

    public BizException(String message, Throwable cause) {
        this(ResultCode.BIZ_ERROR, message, cause);
    }

    public BizException(ResultCode resultCode) {
        this(resultCode, resultCode.getMessage(), null);
    }

    public BizException(ResultCode resultCode, String message) {
        this(resultCode, message, null);
    }

    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BizException(message);
        }
    }

    public static void throwIf(boolean condition, ResultCode resultCode) {
        if (condition) {
            throw new BizException(resultCode);
        }
    }

    public static void throwIf(boolean condition, ResultCode resultCode, String message) {
        if (condition) {
            throw new BizException(resultCode, message);
        }
    }

    public static <T> T requireNonNull(T target, String message) {
        if (target == null) {
            throw new BizException(message);
        }
        return target;
    }

    public static <T> T requireNonNull(T target, ResultCode resultCode) {
        if (target == null) {
            throw new BizException(resultCode);
        }
        return target;
    }
}
