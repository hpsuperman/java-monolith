package com.hpsuperman.monolith.common.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
    private final DateTimeFormatter formatter;

    public JacksonConfig(@Value("${spring.jackson.date-format:yyyy-MM-dd HH:mm:ss}") String pattern) {
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(formatter))
                .deserializers(new LocalDateTimeDeserializer(formatter))

                .serializers(new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE))
                .deserializers(new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE));
    }
}
