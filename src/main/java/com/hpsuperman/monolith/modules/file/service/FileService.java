package com.hpsuperman.monolith.modules.file.service;

import com.hpsuperman.monolith.modules.file.dto.FileVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileVO upload(MultipartFile file);
}
