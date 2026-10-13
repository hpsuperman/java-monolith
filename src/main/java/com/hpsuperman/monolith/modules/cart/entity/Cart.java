package com.hpsuperman.monolith.modules.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hpsuperman.monolith.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@TableName("sys_cart")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Cart extends BaseEntity {
    private Long userId;
    private Long productId;
    private Integer quantity;
}
