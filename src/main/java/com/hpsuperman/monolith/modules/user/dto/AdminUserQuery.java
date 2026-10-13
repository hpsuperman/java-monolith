package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.common.dto.PageQuery;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserQuery extends PageQuery {
    private EnabledStatus status;
    private String keyword;
}
