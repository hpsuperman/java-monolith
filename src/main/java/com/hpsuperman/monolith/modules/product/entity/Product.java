package com.hpsuperman.monolith.modules.product.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hpsuperman.monolith.common.entity.BaseEntity;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@TableName("sys_product")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Product extends BaseEntity {
    private String name;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private String mainImage;
    private String detail;
    private Long categoryId;
    private EnabledStatus status;

    @TableLogic
    private Integer deleted;
}
