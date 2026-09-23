package com.hpsuperman.monolith.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.minio")
public class MinioProperties {
    private String endpoint = "http://127.0.0.1:9000";

    private String accessKey = "minioadmin";

    private String secretKey = "minioadmin";

    private String bucket = "monolith";

    private String publicUrl;

    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    private List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
}
