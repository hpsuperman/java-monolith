package com.hpsuperman.monolith.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI monolithOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java Monolith API")
                        .description("""
                                单体服务脚手架接口文档。

                                调用受保护接口前，先调 `POST /api/auth/login` 拿到 accessToken，
                                再点右上角 Authorize 把 token 粘进去（只填 token 本身，不要加 Bearer 前缀）。
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("hpsuperman")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("粘贴登录接口返回的 accessToken")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
