package com.hpsuperman.monolith.modules.material.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "更新物料请求")
public class MaterialUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 128, message = "物料名称最长 128 位")
    @Schema(description = "物料名称，不传表示不修改；提交了就不能是空白")
    private String name;

    @Size(max = 128, message = "规格型号最长 128 位")
    @Schema(description = "规格型号，传空串表示清空")
    private String spec;

    @Size(max = 16, message = "计量单位最长 16 位")
    @Schema(description = "计量单位，不传表示不修改；提交了就不能是空白")
    private String unit;

    @Size(max = 64, message = "物料分类最长 64 位")
    @Schema(description = "物料分类，传空串表示清空")
    private String category;

    @Schema(description = "状态：1=启用，0=停用；不传表示不修改", allowableValues = {"0", "1"})
    private EnabledStatus status;

    @DecimalMin(value = "0", message = "参考单价不能为负数")
    @Digits(integer = 14, fraction = 4, message = "参考单价最多 14 位整数、4 位小数")
    @Schema(description = "参考单价，传 0 表示清空；无法清回「未维护」")
    private BigDecimal price;

    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "币种需为 3 位大写字母的 ISO 4217 码，如 CNY")
    @Schema(description = "币种，传空串表示清空")
    private String currency;

    @Min(value = 0, message = "当前库存不能为负数")
    @Schema(description = "当前库存。⚠️ 本模块不做出入库校验，仅作档案记录")
    private Integer stock;

    @Min(value = 0, message = "安全库存不能为负数")
    @Schema(description = "安全库存阈值，传 0 表示清空")
    private Integer safetyStock;

    @Size(max = 128, message = "供应商最长 128 位")
    @Schema(description = "供应商，传空串表示清空")
    private String supplier;

    @Min(value = 0, message = "采购周期不能为负数")
    @Max(value = 3650, message = "采购周期最长 3650 天")
    @Schema(description = "采购周期（天），传 0 表示清空")
    private Integer purchaseCycle;

    @DecimalMin(value = "0", message = "采购价不能为负数")
    @Digits(integer = 14, fraction = 4, message = "采购价最多 14 位整数、4 位小数")
    @Schema(description = "采购价，传 0 表示清空；无法清回「未维护」")
    private BigDecimal purchasePrice;

    @Size(max = 512, message = "备注最长 512 位")
    @Schema(description = "备注，传空串表示清空")
    private String remark;

    @Size(max = 512, message = "图片地址最长 512 位")
    @Schema(description = "图片地址，传空串表示清空")
    private String imageUrl;
}
