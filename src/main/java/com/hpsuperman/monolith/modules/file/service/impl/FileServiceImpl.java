package com.hpsuperman.monolith.modules.file.service.impl;

import com.hpsuperman.monolith.common.config.MinioProperties;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.modules.file.dto.FileVO;
import com.hpsuperman.monolith.modules.file.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int HEADER_LENGTH = 12;

    private record Signature(int offset, byte[] bytes) {
    }

    private static final Map<String, List<Signature>> SIGNATURES = Map.of(
        "jpg", List.of(new Signature(0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})),
        "jpeg", List.of(new Signature(0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})),
        "png", List.of(new Signature(0, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})),
        "gif", List.of(new Signature(0, new byte[]{'G', 'I', 'F', '8'})),
        "bmp", List.of(new Signature(0, new byte[]{'B', 'M'})),
        "webp", List.of(
            new Signature(0, new byte[]{'R', 'I', 'F', 'F'}),
            new Signature(8, new byte[]{'W', 'E', 'B', 'P'})));

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public FileVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件不能为空");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new BizException("不支持的文件类型");
        }

        if (file.getSize() > properties.getMaxFileSize().toBytes()) {
            throw new BizException("文件超出大小限制");
        }

        if (!matchesSignature(extension, readHeader(file))) {
            throw new BizException("文件内容与扩展名不符");
        }

        String objectName = LocalDate.now().format(DATE_PATH)
            + "/" + UUID.randomUUID() + "." + extension;

        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectName)
                .stream(in, file.getSize(), -1)
                .build());
        } catch (Exception e) {
            log.error("上传到 MinIO 失败 | object={}", objectName, e);
            throw new BizException("文件上传失败");
        }

        FileVO vo = new FileVO();
        vo.setUrl(publicUrl(objectName));
        return vo;
    }

    private String resolveExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readHeader(MultipartFile file) {
        byte[] header = new byte[HEADER_LENGTH];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(header, 0, HEADER_LENGTH);
            return read < HEADER_LENGTH ? Arrays.copyOf(header, Math.max(read, 0)) : header;
        } catch (IOException e) {
            throw new BizException("文件读取失败");
        }
    }

    private boolean matchesSignature(String extension, byte[] header) {
        List<Signature> signatures = SIGNATURES.get(extension);
        if (signatures == null) {
            return false;
        }
        for (Signature signature : signatures) {
            if (!matchesAt(header, signature)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAt(byte[] header, Signature signature) {
        int offset = signature.offset();
        byte[] bytes = signature.bytes();
        if (header.length < offset + bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if (header[offset + i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private String publicUrl(String objectName) {
        String base = StringUtils.hasText(properties.getPublicUrl())
            ? properties.getPublicUrl()
            : properties.getEndpoint();
        return base.replaceAll("/+$", "") + "/" + properties.getBucket() + "/" + objectName;
    }
}
