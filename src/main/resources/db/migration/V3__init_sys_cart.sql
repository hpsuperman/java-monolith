CREATE TABLE sys_cart
(
    id          BIGINT   NOT NULL COMMENT '主键，自动生成',
    user_id     BIGINT   NOT NULL COMMENT '用户 id',
    product_id  BIGINT   NOT NULL COMMENT '商品 id',
    quantity    INT      NOT NULL DEFAULT 1 COMMENT '数量',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
    COMMENT ='购物车表'
