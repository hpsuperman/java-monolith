package com.hpsuperman.monolith.modules.product.dto;

import com.hpsuperman.monolith.common.dto.PageQuery;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminProductQuery extends PageQuery {
    private EnabledStatus status;
    private String keyword;
    private Long categoryId;
}
