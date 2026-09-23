package com.hpsuperman.monolith.modules.material.dto;

import com.hpsuperman.monolith.common.dto.PageQuery;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "物料分页查询条件")
public class MaterialQueryRequest extends PageQuery {
    private static final long serialVersionUID = 1L;

    @Schema(description = "物料编码，模糊匹配。列校对规则不区分大小写，传 ab 能匹配到 AB-001")
    private String code;

    @Schema(description = "物料名称，模糊匹配")
    private String name;

    @Schema(description = "物料分类，精确匹配")
    private String category;

    @Schema(description = "状态：1=启用，0=停用", allowableValues = {"0", "1"})
    private EnabledStatus status;

    @Schema(description = "供应商，模糊匹配")
    private String supplier;
}
