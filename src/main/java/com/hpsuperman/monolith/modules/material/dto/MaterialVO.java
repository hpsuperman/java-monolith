package com.hpsuperman.monolith.modules.material.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "物料信息")
public class MaterialVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "物料 ID")
    private Long id;

    @Schema(description = "物料编码")
    private String code;

    @Schema(description = "物料名称")
    private String name;

    @Schema(description = "规格型号")
    private String spec;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "物料分类")
    private String category;

    @Schema(description = "状态：1=启用，0=停用", allowableValues = {"0", "1"})
    private EnabledStatus status;

    @Schema(description = "参考单价")
    private BigDecimal price;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "当前库存。⚠️ 本模块不做出入库校验，仅作档案记录")
    private Integer stock;

    @Schema(description = "安全库存阈值")
    private Integer safetyStock;

    @Schema(description = "供应商")
    private String supplier;

    @Schema(description = "采购周期（天）")
    private Integer purchaseCycle;

    @Schema(description = "采购价")
    private BigDecimal purchasePrice;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "图片地址")
    private String imageUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
