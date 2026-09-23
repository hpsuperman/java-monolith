package com.hpsuperman.monolith.modules.file.controller;

import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.modules.file.dto.FileUploadVO;
import com.hpsuperman.monolith.modules.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件", description = "上传到 MinIO，返回可直接访问的 URL")
@RestController
@RequestMapping("/api/files")
@Validated
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @Operation(summary = "上传文件",
            description = "multipart/form-data，字段名 file。"
                    + "服务端按扩展名白名单 + 文件头魔数双重校验，"
                    + "不采信客户端传来的 filename 与 Content-Type。"
                    + "返回的 url 直接存进业务表（如 biz_material.image_url）即可。")
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }
}
