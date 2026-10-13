package com.hpsuperman.monolith.modules.product.dto;

import com.hpsuperman.monolith.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductQuery extends PageQuery {
    private String keyword;
    private Long categoryId;
}
