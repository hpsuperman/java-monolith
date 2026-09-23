package com.hpsuperman.monolith.modules.file.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hpsuperman.monolith.common.config.MinioProperties;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.common.security.SecurityUtils;
import com.hpsuperman.monolith.modules.file.dto.FileUploadVO;
import com.hpsuperman.monolith.modules.file.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final int HEADER_LENGTH = 32;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp",
            "bmp", "image/bmp");

    private static final Map<String, List<Signature>> SIGNATURES = Map.of(
            "jpg", List.of(new Signature(0, 0xFF, 0xD8, 0xFF)),
            "jpeg", List.of(new Signature(0, 0xFF, 0xD8, 0xFF)),
            "png", List.of(new Signature(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
            "gif", List.of(new Signature(0, 0x47, 0x49, 0x46, 0x38)),
            "bmp", List.of(new Signature(0, 0x42, 0x4D)),
            "webp", List.of(
                    new Signature(0, 0x52, 0x49, 0x46, 0x46),
                    new Signature(8, 0x57, 0x45, 0x42, 0x50)));

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public FileUploadVO upload(MultipartFile file) {
        long size = requireAcceptableSize(file);

        String extension = extensionOf(file.getOriginalFilename());
        requireAllowedExtension(extension);

        String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
        String objectKey = LocalDate.now().format(DATE_DIR) + "/" + IdWorker.getId() + "." + extension;

        try (InputStream raw = file.getInputStream();
             BufferedInputStream in = new BufferedInputStream(raw)) {
            in.mark(HEADER_LENGTH * 2);
            byte[] header = in.readNBytes(HEADER_LENGTH);
            in.reset();

            requireMatchingSignature(header, extension);

            putObject(objectKey, in, size, contentType);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取上传文件失败 | originalName={}", file.getOriginalFilename(), e);
            throw new BizException("文件上传失败，请稍后重试");
        }

        String operator = SecurityUtils.getUsernameOrNull();
        log.info("文件上传成功 | objectKey={} | size={} | operator={}", objectKey, size, operator);

        return FileUploadVO.builder()
                .objectKey(objectKey)
                .url(buildUrl(objectKey))
                .originalName(file.getOriginalFilename())
                .size(size)
                .contentType(contentType)
                .build();
    }

    private long requireAcceptableSize(MultipartFile file) {
        BizException.throwIf(file == null || file.isEmpty(), "上传文件不能为空");

        long size = file.getSize();
        long max = properties.getMaxFileSize().toBytes();
        BizException.throwIf(size > max, "文件不能超过 " + properties.getMaxFileSize().toMegabytes() + "MB");

        return size;
    }

    private static String extensionOf(String originalFilename) {
        BizException.throwIf(!StringUtils.hasText(originalFilename), "文件名不能为空");

        int dot = originalFilename.lastIndexOf('.');
        BizException.throwIf(dot < 0 || dot == originalFilename.length() - 1, "文件缺少扩展名");

        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void requireAllowedExtension(String extension) {
        Set<String> allowed = properties.getAllowedExtensions().stream()
                .map(item -> item.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toUnmodifiableSet());

        BizException.throwIf(!allowed.contains(extension),
                "不支持的文件类型：." + extension + "，允许 " + String.join("/", allowed));
    }

    private static void requireMatchingSignature(byte[] header, String extension) {
        List<Signature> signatures = SIGNATURES.get(extension);
        if (signatures == null) {
            return;
        }

        boolean matched = signatures.stream().allMatch(signature -> signature.matches(header));
        BizException.throwIf(!matched, "文件内容与扩展名 ." + extension + " 不符");
    }

    private void putObject(String objectKey, InputStream in, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() == null ? null : e.errorResponse().code();
            if ("NoSuchBucket".equals(code)) {
                log.error("bucket 不存在 | bucket={}，请先创建并设置公开读策略", properties.getBucket());
                throw new BizException("存储桶不存在，请联系管理员");
            }
            if ("AccessDenied".equals(code)) {
                log.error("bucket 拒绝写入 | bucket={} | accessKey={}",
                        properties.getBucket(), properties.getAccessKey());
                throw new BizException("对象存储拒绝写入，请联系管理员");
            }
            log.error("对象存储返回错误 | objectKey={} | code={}", objectKey, code, e);
            throw new BizException("文件上传失败，请稍后重试");
        } catch (Exception e) {
            log.error("对象存储不可用 | endpoint={} | objectKey={}", properties.getEndpoint(), objectKey, e);
            throw new BizException("对象存储不可用，请稍后重试");
        }
    }

    private String buildUrl(String objectKey) {
        String base = StringUtils.hasText(properties.getPublicUrl())
                ? properties.getPublicUrl()
                : properties.getEndpoint();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        return base + "/" + properties.getBucket() + "/" + objectKey;
    }

    private record Signature(int offset, int... magic) {
        boolean matches(byte[] header) {
            if (header.length < offset + magic.length) {
                return false;
            }
            for (int i = 0; i < magic.length; i++) {
                if ((header[offset + i] & 0xFF) != magic[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
