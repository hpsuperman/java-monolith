package com.hpsuperman.monolith.modules.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class AdminProductVO {
    private Long id;
    private String name;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private String mainImage;
    private String detail;
    private Long categoryId;
    private EnabledStatus status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
