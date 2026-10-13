package com.hpsuperman.monolith.modules.user.service;

import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.modules.user.dto.AdminUserQuery;
import com.hpsuperman.monolith.modules.user.dto.AdminUserVO;
import com.hpsuperman.monolith.modules.user.dto.CreateUserRequest;
import com.hpsuperman.monolith.modules.user.dto.UpdateUserRequest;

public interface AdminUserService {
    PageResult<AdminUserVO> list(AdminUserQuery query);
    AdminUserVO get(Long userId);
    AdminUserVO create(CreateUserRequest request);
    void update(Long operatorId, Long id, UpdateUserRequest request);
    void delete(Long operatorId, Long id);
}
