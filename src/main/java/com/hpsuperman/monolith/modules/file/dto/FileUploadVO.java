package com.hpsuperman.monolith.modules.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@Schema(description = "文件上传结果")
public class FileUploadVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "对象存储中的路径，删除文件时用它，不要从 url 里截")
    private String objectKey;

    @Schema(description = "可直接访问的 URL，存进业务表的就是这个")
    private String url;

    @Schema(description = "原始文件名，仅回显，不参与存储路径")
    private String originalName;

    @Schema(description = "文件字节数")
    private long size;

    @Schema(description = "MIME 类型，由服务端按扩展名判定，不采信客户端")
    private String contentType;
}
