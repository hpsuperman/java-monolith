-- ============================================================
-- V1：初始用户表
--
-- 由应用启动时自动执行（见 application.yml 的 spring.flyway）。
-- 不需要手工跑，也不再需要 CREATE DATABASE——库由 docker-compose 的
-- MYSQL_DATABASE 建好，或本地手工 CREATE DATABASE monolith 一次。
--
-- ⚠️ 本文件一旦在任何环境（包括同事的机器）执行过，就不要再修改它。
--    Flyway 会为每个迁移文件存一份校验和，改动会让所有执行过它的库
--    在启动时报 "Migration checksum mismatch" 而直接起不来。
--    要改表结构，请新建 V3__xxx.sql，不要动这里。
--
-- 为什么 V1 只有 sys_user、biz_material 在 V2：
--    存量库（老 schema.sql 建的）里只有 sys_user 这一张表。
--    baseline-on-migrate 会把这样的库基线到 V1，然后正好只执行 V2 把物料表补上，
--    全程无需人工干预。若两张表都塞进 V1，存量库基线完就再也不会执行任何迁移，
--    biz_material 永远建不出来，而且不报任何错。
-- ============================================================

CREATE TABLE `sys_user`
(
    `id`          BIGINT       NOT NULL COMMENT '主键，雪花 ID（MyBatis-Plus ASSIGN_ID）',
    `username`    VARCHAR(32)  NOT NULL COMMENT '登录名',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文，长度固定 60，留 100 有余量',
    `nickname`    VARCHAR(32)           DEFAULT NULL COMMENT '昵称',
    `email`       VARCHAR(128)          DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=禁用',
    `roles`       VARCHAR(255)          DEFAULT 'USER' COMMENT '角色码，逗号分隔。脚手架简化实现，正式项目请换 RBAC 三张表',
    -- ⚠️ deleted / version 的 NOT NULL DEFAULT 0 不能省：这两个字段没有标
    --    @TableField(fill = ...)，不在 MyMetaObjectHandler 的自动填充范围内，
    --    全靠这里的数据库默认值兜底（见 MyMetaObjectHandler 的类注释）
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删，1=已删',
    `version`     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT NULL COMMENT '更新时间',
    `create_by`   BIGINT                DEFAULT NULL COMMENT '创建人 ID',
    `update_by`   BIGINT                DEFAULT NULL COMMENT '更新人 ID',
    PRIMARY KEY (`id`),
    -- 注意：逻辑删除后该用户名仍被占用（deleted=1 的记录也参与唯一约束）。
    -- 若业务上需要「删除后可重新注册同名账号」，把 deleted 改存删除时间戳
    -- （0 表示未删，非 0 表示删除时刻），并把唯一键改成 (username, deleted)。
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表';

-- 默认管理员账号由应用启动时自动创建（见 AdminUserInitializer）：
--   admin / admin123  角色 ADMIN,USER
-- 之所以不在这里写死 BCrypt 密文，是为了避免密文与实际使用的
-- PasswordEncoder 不匹配导致登录失败——运行时生成才能保证一致。
