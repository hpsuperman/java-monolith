package com.hpsuperman.monolith.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录状态已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    PAYLOAD_TOO_LARGE(413, "请求体过大"),
    INTERNAL_ERROR(500, "系统内部错误，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    BIZ_ERROR(500, "业务处理失败"),
    DATA_CONFLICT(500, "数据已存在或状态冲突");

    private final int code;
    private final String message;
}
