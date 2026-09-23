CREATE TABLE sys_user
(
    id          BIGINT       NOT NULL COMMENT '主键，自动生成',
    phone       VARCHAR(11)  NOT NULL COMMENT '手机号，登录用',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    nickname    VARCHAR(100) NULL COMMENT '昵称',
    email       VARCHAR(100) NULL COMMENT '邮箱',
    roles       VARCHAR(100) NOT NULL DEFAULT 'USER' COMMENT '角色，逗号分隔',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0 禁用 1 启用',
    deleted     INT          NOT NULL DEFAULT 1 COMMENT '0 删除 1 正常',
    create_time DATETIME     NOT NULL COMMENT '创建时间',
    update_time DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT = '用户表';
