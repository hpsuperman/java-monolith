CREATE TABLE sys_product
(
    id          BIGINT         NOT NULL COMMENT '主键，自动生成',
    name        VARCHAR(200)   NOT NULL COMMENT '商品名',
    subtitle    VARCHAR(200)   NULL COMMENT '副标题',
    price       DECIMAL(10, 2) NOT NULL COMMENT '售价，单位元',
    stock       INT            NOT NULL DEFAULT 0 COMMENT '库存',
    main_image  VARCHAR(500)   NULL COMMENT '主图 URL',
    detail      TEXT           NULL COMMENT '图文详情',
    category_id BIGINT         NULL COMMENT '分类 id，分类表暂不建',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '0 下架 1 上架',
    deleted     TINYINT        NOT NULL DEFAULT 0 COMMENT '0 未删 1 已删',
    create_time DATETIME       NOT NULL COMMENT '创建时间',
    update_time DATETIME       NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_create_time (status, create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT = '商品表';
