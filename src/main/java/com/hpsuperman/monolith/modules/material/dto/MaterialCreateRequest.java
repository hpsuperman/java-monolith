package com.hpsuperman.monolith.modules.material.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "创建物料请求")
public class MaterialCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "物料编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{1,63}$",
        message = "物料编码只能是 2-64 位字母、数字、中划线或下划线，且不能以中划线/下划线开头")
    @Schema(description = "物料编码，创建后不可修改", example = "M-BOLT-001")
    private String code;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 128, message = "物料名称最长 128 位")
    @Schema(description = "物料名称", example = "六角螺栓 M8x40")
    private String name;

    @Size(max = 128, message = "规格型号最长 128 位")
    @Schema(description = "规格型号", example = "M8x40 不锈钢 304")
    private String spec;

    @NotBlank(message = "计量单位不能为空")
    @Size(max = 16, message = "计量单位最长 16 位")
    @Schema(description = "计量单位", example = "个")
    private String unit;

    @Size(max = 64, message = "物料分类最长 64 位")
    @Schema(description = "物料分类，自由文本。正式项目建议换字典表，见实体注释", example = "紧固件")
    private String category;

    @Schema(description = "状态：1=启用，0=停用；不传默认 1", allowableValues = {"0", "1"})
    private EnabledStatus status;

    @DecimalMin(value = "0", message = "参考单价不能为负数")
    @Digits(integer = 14, fraction = 4, message = "参考单价最多 14 位整数、4 位小数")
    @Schema(description = "参考单价，不传表示未维护", example = "0.5000")
    private BigDecimal price;

    @Pattern(regexp = "^$|^[A-Z]{3}$", message = "币种需为 3 位大写字母的 ISO 4217 码，如 CNY")
    @Schema(description = "币种，不传表示未指定", example = "CNY")
    private String currency;

    @Min(value = 0, message = "当前库存不能为负数")
    @Schema(description = "当前库存。⚠️ 本模块不做出入库校验，仅作档案记录", example = "100")
    private Integer stock;

    @Min(value = 0, message = "安全库存不能为负数")
    @Schema(description = "安全库存阈值，不传表示未设置", example = "20")
    private Integer safetyStock;

    @Size(max = 128, message = "供应商最长 128 位")
    @Schema(description = "供应商，自由文本", example = "某某五金")
    private String supplier;

    @Min(value = 0, message = "采购周期不能为负数")
    @Max(value = 3650, message = "采购周期最长 3650 天")
    @Schema(description = "采购周期（天），不传表示未维护", example = "7")
    private Integer purchaseCycle;

    @DecimalMin(value = "0", message = "采购价不能为负数")
    @Digits(integer = 14, fraction = 4, message = "采购价最多 14 位整数、4 位小数")
    @Schema(description = "采购价，不传表示未维护", example = "0.3000")
    private BigDecimal purchasePrice;

    @Size(max = 512, message = "备注最长 512 位")
    @Schema(description = "备注")
    private String remark;

    @Size(max = 512, message = "图片地址最长 512 位")
    @Schema(description = "图片地址，本模块只存 URL，不提供上传接口")
    private String imageUrl;
}
