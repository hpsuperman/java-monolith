package com.hpsuperman.monolith.modules.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ProductVO {
    private Long id;
    private String name;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private String mainImage;
    private String detail;
    private Long categoryId;
    private LocalDateTime createTime;
}
