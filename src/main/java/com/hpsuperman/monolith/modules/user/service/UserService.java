package com.hpsuperman.monolith.modules.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.modules.user.dto.UserCreateRequest;
import com.hpsuperman.monolith.modules.user.dto.UserQueryRequest;
import com.hpsuperman.monolith.modules.user.dto.UserUpdateRequest;
import com.hpsuperman.monolith.modules.user.dto.UserVO;
import com.hpsuperman.monolith.modules.user.entity.User;

public interface UserService extends IService<User> {
    PageResult<UserVO> page(UserQueryRequest query);

    UserVO getDetail(Long id);

    UserVO create(UserCreateRequest request);

    UserVO update(Long id, UserUpdateRequest request);

    void delete(Long id);

    User getByUsername(String username);
}
