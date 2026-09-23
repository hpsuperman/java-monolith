package com.hpsuperman.monolith.common.config;

import com.hpsuperman.monolith.common.enums.EnabledStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new StringToEnabledStatusConverter());
    }

    static class StringToEnabledStatusConverter implements Converter<String, EnabledStatus> {
        @Override
        public EnabledStatus convert(@NonNull String source) {
            String trimmed = source.trim();
            return trimmed.isEmpty() ? null : EnabledStatus.of(Integer.valueOf(trimmed));
        }
    }
}
