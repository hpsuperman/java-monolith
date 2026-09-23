package com.hpsuperman.monolith.modules.file.service;

import com.hpsuperman.monolith.modules.file.dto.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileUploadVO upload(MultipartFile file);
}
