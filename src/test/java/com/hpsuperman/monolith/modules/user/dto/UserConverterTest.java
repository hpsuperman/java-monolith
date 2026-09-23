package com.hpsuperman.monolith.modules.user.dto;

import com.hpsuperman.monolith.modules.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserConverter")
class UserConverterTest {
    @Nested
    @DisplayName("joinRoles")
    class JoinRoles {
        @Test
        @DisplayName("空输入一律返回空串，不是 null——返回 null 会被 update-strategy: not_null 整个略过")
        void returnsEmptyStringInsteadOfNull() {
            assertThat(UserConverter.joinRoles(null)).isEmpty();
            assertThat(UserConverter.joinRoles(List.of())).isEmpty();

            assertThat(UserConverter.joinRoles(List.of(""))).isEmpty();
            assertThat(UserConverter.joinRoles(List.of("  "))).isEmpty();
            assertThat(UserConverter.joinRoles(Arrays.asList(null, " "))).isEmpty();
        }

        @Test
        @DisplayName("去除空白与重复项")
        void trimsAndDeduplicates() {
            assertThat(UserConverter.joinRoles(List.of(" ADMIN ", "USER", "ADMIN")))
                    .isEqualTo("ADMIN,USER");
        }

        @Test
        @DisplayName("create 依赖 hasText(空串) 为 false 来回落默认角色")
        void emptyResultStillFallsBackToDefaultRole() {
            assertThat(org.springframework.util.StringUtils.hasText(UserConverter.joinRoles(List.of())))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("parseRoles")
    class ParseRoles {
        @Test
        @DisplayName("逗号分隔串拆成列表，忽略空白项")
        void splitsAndIgnoresBlanks() {
            assertThat(UserConverter.parseRoles("ADMIN, USER")).containsExactly("ADMIN", "USER");
            assertThat(UserConverter.parseRoles("ADMIN,,USER")).containsExactly("ADMIN", "USER");
            assertThat(UserConverter.parseRoles("")).isEmpty();
            assertThat(UserConverter.parseRoles(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("canonicalRoles")
    class CanonicalRoles {
        @Test
        @DisplayName("顺序不同不算变更——前端多选框重排不该把人踢下线")
        void ignoresOrder() {
            assertThat(UserConverter.canonicalRoles("USER,ADMIN"))
                    .isEqualTo(UserConverter.canonicalRoles("ADMIN,USER"));
        }

        @Test
        @DisplayName("ROLE_ 前缀不算变更——LoginUser 对两者授予同一权限")
        void ignoresRolePrefix() {
            assertThat(UserConverter.canonicalRoles("ROLE_ADMIN,USER"))
                    .isEqualTo(UserConverter.canonicalRoles("ADMIN,USER"));
        }

        @Test
        @DisplayName("重复项与空白不算变更")
        void ignoresDuplicatesAndWhitespace() {
            assertThat(UserConverter.canonicalRoles(" ADMIN , ADMIN , USER "))
                    .isEqualTo(UserConverter.canonicalRoles("ADMIN,USER"));
        }

        @Test
        @DisplayName("真的增减角色要能看出来")
        void detectsRealChanges() {
            assertThat(UserConverter.canonicalRoles("ADMIN"))
                    .isNotEqualTo(UserConverter.canonicalRoles("ADMIN,USER"));
            assertThat(UserConverter.canonicalRoles("ADMIN,USER"))
                    .isNotEqualTo(UserConverter.canonicalRoles("USER"));
            assertThat(UserConverter.canonicalRoles("SUPER_ADMIN"))
                    .isNotEqualTo(UserConverter.canonicalRoles("ADMIN"));
        }
    }

    @Nested
    @DisplayName("hasRole")
    class HasRole {
        @Test
        @DisplayName("按角色码整体匹配，不做子串匹配")
        void matchesWholeRoleOnly() {
            assertThat(UserConverter.hasRole("ADMIN,USER", "ADMIN")).isTrue();
            assertThat(UserConverter.hasRole("ROLE_ADMIN", "ADMIN")).isTrue();
            assertThat(UserConverter.hasRole("USER", "ADMIN")).isFalse();

            assertThat(UserConverter.hasRole("SUPER_ADMIN", "ADMIN")).isFalse();
            assertThat(UserConverter.hasRole(null, "ADMIN")).isFalse();
        }
    }

    @Test
    @DisplayName("toVO 把角色串转成列表；空角色是空列表而不是 null")
    void toVOExposesRoles() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$hash");
        user.setRoles("ADMIN,USER");

        UserVO vo = UserConverter.toVO(user);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getRoles()).containsExactly("ADMIN", "USER");
        assertThat(UserConverter.toVO(null)).isNull();

        user.setRoles(null);

        assertThat(UserConverter.toVO(user).getRoles()).isEmpty();
    }
}
