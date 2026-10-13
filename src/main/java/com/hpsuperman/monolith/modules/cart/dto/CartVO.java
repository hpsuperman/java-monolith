package com.hpsuperman.monolith.modules.cart.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class CartVO {
    private Long productId;
    private String name;
    private String mainImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;
    private Integer stock;
    private EnabledStatus status;
}
