package com.hpsuperman.monolith.modules.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.PageResult;
import com.hpsuperman.monolith.common.result.ResultCode;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import com.hpsuperman.monolith.common.security.TokenStore;
import com.hpsuperman.monolith.modules.user.dto.UserConverter;
import com.hpsuperman.monolith.modules.user.dto.UserCreateRequest;
import com.hpsuperman.monolith.modules.user.dto.UserQueryRequest;
import com.hpsuperman.monolith.modules.user.dto.UserUpdateRequest;
import com.hpsuperman.monolith.modules.user.dto.UserVO;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.mapper.UserMapper;
import com.hpsuperman.monolith.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private static final String DEFAULT_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserVO> page(UserQueryRequest query) {
        var wrapper = Wrappers.<User>lambdaQuery()
            .like(StringUtils.hasText(query.getUsername()), User::getUsername, query.getUsername())
            .like(StringUtils.hasText(query.getNickname()), User::getNickname, query.getNickname())
            .eq(query.getStatus() != null, User::getStatus, query.getStatus())

            .orderByDesc(User::getCreateTime)
            .orderByDesc(User::getId);

        Page<User> page = page(query.toPage(), wrapper);
        return PageResult.of(page, UserConverter::toVO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO getDetail(Long id) {
        User user = getById(id);
        BizException.requireNonNull(user, "用户不存在");
        return UserConverter.toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateRequest request) {
        long existing = count(Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        BizException.throwIf(existing > 0, "用户名已存在");

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
            ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        user.setStatus(EnabledStatus.ENABLED);

        String roles = UserConverter.joinRoles(request.getRoles());
        user.setRoles(StringUtils.hasText(roles) ? roles : DEFAULT_ROLE);

        boolean saved = save(user);
        BizException.throwIf(!saved, "用户创建失败");

        log.info("创建用户成功 | id={} | username={}", user.getId(), user.getUsername());
        return UserConverter.toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO update(Long id, UserUpdateRequest request) {
        User existing = getById(id);
        BizException.requireNonNull(existing, "用户不存在");

        String newRoles = request.getRoles() == null
            ? null
            : UserConverter.joinRoles(request.getRoles());

        guardSelfUpdate(id, request.getStatus(), newRoles);

        User update = new User();
        update.setId(id);

        update.setVersion(existing.getVersion());
        update.setNickname(request.getNickname());
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());
        update.setStatus(request.getStatus());
        if (newRoles != null) {
            update.setRoles(newRoles);
        }

        boolean updated = updateById(update);

        BizException.throwIf(!updated, ResultCode.DATA_CONFLICT, "数据已被他人修改，请刷新后重试");

        if (shouldRevokeTokens(existing, request.getStatus(), newRoles)) {
            revokeAfterCommit(id);
        }

        return UserConverter.toVO(getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        User existing = getById(id);
        BizException.requireNonNull(existing, "用户不存在");

        BizException.throwIf(id.equals(SecurityUtils.getUserId()), "不能删除当前登录账号");

        boolean removed = removeById(id);
        BizException.throwIf(!removed, "删除失败");

        log.info("删除用户 | id={} | operator={}", id, SecurityUtils.getUsernameOrNull());

        revokeAfterCommit(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return getOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username), false);
    }

    private void guardSelfUpdate(Long targetId, EnabledStatus newStatus, String newRoles) {
        if (!targetId.equals(SecurityUtils.getUserId())) {
            return;
        }

        BizException.throwIf(newStatus == EnabledStatus.DISABLED, "不能禁用当前登录账号");
        if (newRoles != null) {
            BizException.throwIf(!UserConverter.hasRole(newRoles, ADMIN_ROLE), "不能移除自己的管理员角色");
        }
    }

    private boolean shouldRevokeTokens(User existing, EnabledStatus newStatus, String newRoles) {
        boolean justDisabled = newStatus == EnabledStatus.DISABLED
            && existing.getStatus() != EnabledStatus.DISABLED;
        boolean rolesChanged = newRoles != null
            && !UserConverter.canonicalRoles(newRoles)
            .equals(UserConverter.canonicalRoles(existing.getRoles()));
        return justDisabled || rolesChanged;
    }

    private void revokeAfterCommit(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            tokenStore.revokeUserTokens(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tokenStore.revokeUserTokens(userId);
            }
        });
    }
}
