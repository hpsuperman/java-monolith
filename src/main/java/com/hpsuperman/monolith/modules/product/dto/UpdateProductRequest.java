package com.hpsuperman.monolith.modules.product.dto;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    @Size(max = 200, message = "商品名最多 200 字")
    private String name;

    @Size(max = 200, message = "副标题最多 200 字")
    private String subtitle;

    @DecimalMin(value = "0.00", message = "价格不能为负")
    @Digits(integer = 8, fraction = 2, message = "价格最多 8 位整数、2 位小数")
    private BigDecimal price;

    @Min(value = 0, message = "库存不能为负")
    private Integer stock;

    @Size(max = 500, message = "主图 URL 最多 500 字")
    private String mainImage;

    private String detail;

    private Long categoryId;

    private EnabledStatus status;
}
