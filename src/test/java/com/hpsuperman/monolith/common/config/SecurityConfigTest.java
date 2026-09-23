package com.hpsuperman.monolith.common.config;

import com.hpsuperman.monolith.common.security.JwtAuthenticationFilter;
import com.hpsuperman.monolith.common.security.RestAccessDeniedHandler;
import com.hpsuperman.monolith.common.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("SecurityConfig CORS")
class SecurityConfigTest {
    private CorsProperties corsProperties;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
        securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(RestAuthenticationEntryPoint.class),
                mock(RestAccessDeniedHandler.class),
                corsProperties);
    }

    @Test
    @DisplayName("通配符 + 允许凭证时启动直接失败，而不是悄悄降级成反射任意 Origin")
    void failsFastOnWildcardWithCredentials() {
        corsProperties.setAllowedOrigins(List.of("*"));
        corsProperties.setAllowCredentials(true);

        assertThatThrownBy(securityConfig::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins")
                .hasMessageContaining("app.cors.allow-credentials");
    }

    @Test
    @DisplayName("不带凭证时 * 是合法写法，走 allowedOrigins")
    void allowsWildcardWithoutCredentials() {
        corsProperties.setAllowedOrigins(List.of("*"));
        corsProperties.setAllowCredentials(false);

        CorsConfiguration config = resolve();

        assertThat(config.getAllowedOrigins()).containsExactly("*");
        assertThat(config.getAllowedOriginPatterns()).isNull();
        assertThat(config.getAllowCredentials()).isFalse();
    }

    @Test
    @DisplayName("含 * 的域名模式走 allowedOriginPatterns，可携带凭证")
    void mapsSubdomainPatternToOriginPatterns() {
        corsProperties.setAllowedOrigins(List.of("https://*.example.com"));
        corsProperties.setAllowCredentials(true);

        CorsConfiguration config = resolve();

        assertThat(config.getAllowedOriginPatterns()).containsExactly("https://*.example.com");

        assertThat(config.getAllowedOrigins()).isNull();
        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("全部写具体域名时走 allowedOrigins")
    void mapsConcreteOriginsToAllowedOrigins() {
        corsProperties.setAllowedOrigins(List.of("https://app.example.com", "https://admin.example.com"));
        corsProperties.setAllowCredentials(true);

        CorsConfiguration config = resolve();

        assertThat(config.getAllowedOrigins())
                .containsExactly("https://app.example.com", "https://admin.example.com");
        assertThat(config.getAllowedOriginPatterns()).isNull();
    }

    @Test
    @DisplayName("裸 * 混在具体域名里也照样被拦下")
    void detectsWildcardAmongConcreteOrigins() {
        corsProperties.setAllowedOrigins(List.of("https://app.example.com", "*"));
        corsProperties.setAllowCredentials(true);

        assertThatThrownBy(securityConfig::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class);
    }

    private CorsConfiguration resolve() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        return source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/users"));
    }
}
