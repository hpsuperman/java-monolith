package com.hpsuperman.monolith.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hpsuperman.monolith.common.entity.BaseEntity;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("biz_material")
public class Material extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String code;

    private String name;

    private String spec;

    private String unit;

    private String category;

    private EnabledStatus status;

    private BigDecimal price;

    private String currency;

    private Integer stock;

    private Integer safetyStock;

    private String supplier;

    private Integer purchaseCycle;

    private BigDecimal purchasePrice;

    private String remark;

    private String imageUrl;
}
