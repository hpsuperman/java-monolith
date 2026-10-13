package com.hpsuperman.monolith.modules.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.enums.Role;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.ResultCode;
import com.hpsuperman.monolith.modules.user.dto.*;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.mapper.UserMapper;
import com.hpsuperman.monolith.modules.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl extends ServiceImpl<UserMapper, User> implements AdminUserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<AdminUserVO> list(AdminUserQuery query) {
        Page<User> page = page(query.toPage(), Wrappers.<User>lambdaQuery()
            .eq(query.getStatus() != null, User::getStatus, query.getStatus())
            .and(StringUtils.hasText(query.getKeyword()), w -> w
                .like(User::getPhone, query.getKeyword())
                .or()
                .like(User::getNickname, query.getKeyword()))
            .orderByDesc(User::getCreateTime));
        return PageResult.of(page, UserConverter::toAdminVO);
    }

    @Override
    public AdminUserVO get(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return UserConverter.toAdminVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO create(CreateUserRequest request) {
        if (lambdaQuery().eq(User::getPhone, request.getPhone()).exists()) {
            throw new BizException(ResultCode.DATA_CONFLICT, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRoles(request.getRoles() == null || request.getRoles().isEmpty() ? Role.USER.name() : Role.join(request.getRoles()));
        user.setStatus(request.getStatus() == null ? EnabledStatus.ENABLED : request.getStatus());
        save(user);
        return UserConverter.toAdminVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long operatorId, Long id, UpdateUserRequest request) {
        if (request.getNickname() == null && request.getEmail() == null
            && request.getAvatar() == null && request.getRoles() == null
            && request.getStatus() == null) {
            throw new BizException("没有需要修改的内容");
        }
        if (getById(id) == null) {
            throw new BizException("用户不存在");
        }
        if (operatorId.equals(id)) {
            if (request.getStatus() == EnabledStatus.DISABLED) {
                throw new BizException("不能禁用自己");
            }
            if (request.getRoles() != null && !request.getRoles().contains(Role.ADMIN)) {
                throw new BizException("不能摘掉自己的管理员角色");
            }
        }
        User user = new User();
        user.setId(id);
        BeanUtils.copyProperties(request, user);
        user.setRoles(Role.join(request.getRoles()));
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long operatorId, Long id) {
        if (operatorId.equals(id)) {
            throw new BizException("不能删除自己");
        }
        if (!removeById(id)) {
            throw new BizException("用户不存在");
        }
    }
}
