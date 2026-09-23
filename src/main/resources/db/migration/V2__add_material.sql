-- ============================================================
-- V2：物料档案表（modules/material）
-- 纯档案 CRUD：不做库存增减，不做出入库单据流水
--
-- ⚠️ 同 V1：本文件一旦执行过就不要再修改，要改结构请新建 V3__xxx.sql。
--    Flyway 校验的是文件内容的校验和，改一个字符都会让已执行过的库起不来。
--
-- 本文件同时也是「如何追加一个迁移」的样板：加一张表 = 新建一个
-- V3__add_xxx.sql 放同目录 → 重启应用 → 完事。不需要手工执行任何 SQL。
-- ============================================================

CREATE TABLE `biz_material`
(
    `id`             BIGINT        NOT NULL COMMENT '主键，雪花 ID（MyBatis-Plus ASSIGN_ID）',
    `code`           VARCHAR(64)   NOT NULL COMMENT '物料编码，业务唯一标识，创建后不可改',
    `name`           VARCHAR(128)  NOT NULL COMMENT '物料名称',
    `spec`           VARCHAR(128)           DEFAULT NULL COMMENT '规格型号',
    `unit`           VARCHAR(16)   NOT NULL COMMENT '计量单位，如 个/箱/千克',
    `category`       VARCHAR(64)            DEFAULT NULL COMMENT '物料分类，自由文本。暂无字典表，见 README',
    `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1=启用，0=停用',
    `price`          DECIMAL(18,4)          DEFAULT NULL COMMENT '参考单价，NULL=未维护。用 DECIMAL 不用 FLOAT：浮点存金额是经典错误',
    `currency`       VARCHAR(8)             DEFAULT NULL COMMENT '币种，ISO 4217 三字母码，如 CNY',
    `stock`          INT           NOT NULL DEFAULT 0 COMMENT '当前库存。⚠️ 本模块仅作可编辑数字，无任何出入库校验',
    `safety_stock`   INT                    DEFAULT NULL COMMENT '安全库存阈值',
    `supplier`       VARCHAR(128)           DEFAULT NULL COMMENT '供应商，自由文本',
    `purchase_cycle` INT                    DEFAULT NULL COMMENT '采购周期（天）',
    `purchase_price` DECIMAL(18,4)          DEFAULT NULL COMMENT '采购价，NULL=未维护',
    `remark`         VARCHAR(512)           DEFAULT NULL COMMENT '备注',
    `image_url`      VARCHAR(512)           DEFAULT NULL COMMENT '图片地址，只存 URL，本模块不做上传',
    -- 同 V1：NOT NULL DEFAULT 0 是自动填充之外的最后兜底，不能省
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删，1=已删',
    `version`        INT           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME               DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME               DEFAULT NULL COMMENT '更新时间',
    `create_by`      BIGINT                 DEFAULT NULL COMMENT '创建人 ID',
    `update_by`      BIGINT                 DEFAULT NULL COMMENT '更新人 ID',
    PRIMARY KEY (`id`),
    -- 与 sys_user.uk_username 同一个坑：逻辑删除后该编码仍被占用（deleted=1 的记录
    -- 也参与唯一约束）。若业务上需要「删除后可复用编码」，把 deleted 改存删除时间戳，
    -- 并把唯一键改成 (code, deleted)。
    --
    -- 由此派生出一个必须知道的行为差：Service 的查重走 lambdaQuery，
    -- MyBatis-Plus 会自动追加 deleted = 0，所以它只看得到未删记录；
    -- 而唯一键看得到全部记录。于是「编码被一条已删除物料占着」这种情况会绕过
    -- Service 查重、在 INSERT 时撞唯一键，由 GlobalExceptionHandler 的
    -- DuplicateKeyException 分支转成 HTTP 409。这是刻意保留的取舍，不是 bug。
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='物料档案表';
