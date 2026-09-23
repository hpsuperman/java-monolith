package com.hpsuperman.monolith.modules.user.service.impl;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.result.ResultCode;
import com.hpsuperman.monolith.common.security.LoginUser;
import com.hpsuperman.monolith.common.security.TokenStore;
import com.hpsuperman.monolith.modules.user.dto.UserUpdateRequest;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {
    private static final Long SELF_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(passwordEncoder, tokenStore);
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        assertThat(userService.getBaseMapper()).isSameAs(userMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("管理员不能禁用自己")
    void rejectsDisablingSelf() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(SELF_ID)).thenReturn(user(SELF_ID, "ADMIN,USER", EnabledStatus.ENABLED));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setStatus(EnabledStatus.DISABLED);

        assertThatThrownBy(() -> userService.update(SELF_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessage("不能禁用当前登录账号");
    }

    @Test
    @DisplayName("管理员不能摘掉自己的 ADMIN")
    void rejectsRemovingOwnAdmin() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(SELF_ID)).thenReturn(user(SELF_ID, "ADMIN,USER", EnabledStatus.ENABLED));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setRoles(List.of("USER"));

        assertThatThrownBy(() -> userService.update(SELF_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessage("不能移除自己的管理员角色");
    }

    @Test
    @DisplayName("清空自己的角色同样算摘权")
    void rejectsClearingOwnRoles() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(SELF_ID)).thenReturn(user(SELF_ID, "ADMIN,USER", EnabledStatus.ENABLED));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setRoles(List.of());

        assertThatThrownBy(() -> userService.update(SELF_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessage("不能移除自己的管理员角色");
    }

    @Test
    @DisplayName("ROLE_ 前缀、顺序、空白都不算摘权")
    void allowsCosmeticRoleRewrite() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(SELF_ID)).thenReturn(user(SELF_ID, "ADMIN,USER", EnabledStatus.ENABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();

        request.setRoles(List.of(" ROLE_USER ", "ROLE_ADMIN", "USER"));

        assertThat(userService.update(SELF_ID, request).getId()).isEqualTo(SELF_ID);
        verify(tokenStore, never()).revokeUserTokens(anyLong());
    }

    @Test
    @DisplayName("禁用/摘权他人不受影响")
    void allowsModifyingOthers() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "ADMIN,USER", EnabledStatus.ENABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setStatus(EnabledStatus.DISABLED);

        userService.update(OTHER_ID, request);

        verify(tokenStore).revokeUserTokens(OTHER_ID);
    }

    @Test
    @DisplayName("没有认证上下文时守卫必须抛异常，而不是静默放行")
    void failsClosedWithoutAuthentication() {
        when(userMapper.selectById(SELF_ID)).thenReturn(user(SELF_ID, "ADMIN,USER", EnabledStatus.ENABLED));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setStatus(EnabledStatus.DISABLED);

        assertThatThrownBy(() -> userService.update(SELF_ID, request))
                .isInstanceOf(InsufficientAuthenticationException.class);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("原样回传 roles 不撤销——前端 PUT 整对象是常态，不能因此踢人下线")
    void doesNotRevokeWhenRolesUnchanged() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "ADMIN,USER", EnabledStatus.ENABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("新昵称");
        request.setRoles(List.of("ADMIN", "USER"));

        userService.update(OTHER_ID, request);

        verify(tokenStore, never()).revokeUserTokens(anyLong());
    }

    @Test
    @DisplayName("角色真的变了才撤销")
    void revokesWhenRolesReallyChange() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "ADMIN,USER", EnabledStatus.ENABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setRoles(List.of("USER"));

        userService.update(OTHER_ID, request);

        verify(tokenStore).revokeUserTokens(OTHER_ID);
    }

    @Test
    @DisplayName("把禁用账号改回启用不撤销——那不是撤销的触发条件")
    void doesNotRevokeWhenReEnabling() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "USER", EnabledStatus.DISABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setStatus(EnabledStatus.ENABLED);

        userService.update(OTHER_ID, request);

        verify(tokenStore, never()).revokeUserTokens(anyLong());
    }

    @Test
    @DisplayName("更新未提交时不撤销——否则事务回滚了人却已经被踢下线")
    void defersRevocationUntilCommit() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "ADMIN,USER", EnabledStatus.ENABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        TransactionSynchronizationManager.initSynchronization();
        try {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setStatus(EnabledStatus.DISABLED);
            userService.update(OTHER_ID, request);

            verify(tokenStore, never()).revokeUserTokens(anyLong());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(tokenStore).revokeUserTokens(OTHER_ID);
    }

    @Test
    @DisplayName("乐观锁冲突时抛冲突码，且不撤销")
    void doesNotRevokeOnOptimisticLockConflict() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "ADMIN,USER", EnabledStatus.ENABLED));

        when(userMapper.updateById(any(User.class))).thenReturn(0);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setStatus(EnabledStatus.DISABLED);

        assertThatThrownBy(() -> userService.update(OTHER_ID, request))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ResultCode.DATA_CONFLICT.getCode());

        verify(tokenStore, never()).revokeUserTokens(anyLong());
    }

    @Test
    @DisplayName("清空角色传的是空串而不是 null——null 会被 update-strategy 整个略过")
    void writesEmptyStringWhenClearingRoles() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "ADMIN,USER", EnabledStatus.ENABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setRoles(List.of("  "));

        userService.update(OTHER_ID, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRoles()).isEmpty();
    }

    @Test
    @DisplayName("roles 传 null 表示不修改，实体的 roles 保持 null 好让 SQL 略过它")
    void leavesRolesUntouchedWhenNull() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "ADMIN,USER", EnabledStatus.ENABLED));
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest request = new UserUpdateRequest();
        request.setNickname("只改昵称");

        userService.update(OTHER_ID, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRoles()).isNull();
        verify(tokenStore, never()).revokeUserTokens(anyLong());
    }

    @Test
    @DisplayName("不能删除自己")
    void rejectsDeletingSelf() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(SELF_ID)).thenReturn(user(SELF_ID, "ADMIN", EnabledStatus.ENABLED));

        assertThatThrownBy(() -> userService.delete(SELF_ID))
                .isInstanceOf(BizException.class)
                .hasMessage("不能删除当前登录账号");
        verify(userMapper, never()).deleteById(any(Serializable.class));
    }

    @Test
    @DisplayName("删除他人后撤销其令牌——逻辑删除后 refresh 拿不到新令牌，但旧 access token 还活着")
    void revokesTokensOnDelete() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(user(OTHER_ID, "USER", EnabledStatus.ENABLED));
        when(userMapper.deleteById(OTHER_ID)).thenReturn(1);

        userService.delete(OTHER_ID);

        verify(tokenStore).revokeUserTokens(OTHER_ID);
    }

    @Test
    @DisplayName("用户不存在时报错，不撤销任何东西")
    void reportsMissingUserOnDelete() {
        authenticateAs(SELF_ID);
        when(userMapper.selectById(OTHER_ID)).thenReturn(null);

        assertThatThrownBy(() -> userService.delete(OTHER_ID))
                .isInstanceOf(BizException.class)
                .hasMessage("用户不存在");

        verify(tokenStore, never()).revokeUserTokens(anyLong());
    }

    private static void authenticateAs(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "admin", null, "管理员",
                EnabledStatus.ENABLED, List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    private static User user(Long id, String roles, EnabledStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setNickname("用户" + id);
        user.setRoles(roles);
        user.setStatus(status);
        user.setVersion(1);
        return user;
    }
}
