package com.hpsuperman.monolith.modules.file.service.impl;

import com.hpsuperman.monolith.common.config.MinioProperties;
import com.hpsuperman.monolith.common.exception.BizException;
import com.hpsuperman.monolith.modules.file.dto.FileUploadVO;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileServiceImpl")
class FileServiceImplTest {
    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Mock
    private MinioClient minioClient;

    private MinioProperties properties;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setEndpoint("http://127.0.0.1:9000");
        properties.setBucket("monolith");

        fileService = new FileServiceImpl(minioClient, properties);
    }

    @Test
    @DisplayName("空文件被拒")
    void rejectsEmptyFile() {
        MultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(empty))
                .isInstanceOf(BizException.class)
                .hasMessage("上传文件不能为空");
    }

    @Test
    @DisplayName("超过大小上限被拒")
    void rejectsOversizedFile() {
        properties.setMaxFileSize(DataSize.ofBytes(8));

        assertThatThrownBy(() -> fileService.upload(png("a.png", 64)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("文件不能超过");
    }

    @Test
    @DisplayName("没有扩展名被拒")
    void rejectsFileWithoutExtension() {
        assertThatThrownBy(() -> fileService.upload(png("README", 64)))
                .isInstanceOf(BizException.class)
                .hasMessage("文件缺少扩展名");
    }

    @Test
    @DisplayName("扩展名不在白名单被拒")
    void rejectsDisallowedExtension() {
        assertThatThrownBy(() -> fileService.upload(png("shell.jsp", 64)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持的文件类型：.jsp");
    }

    @Test
    @DisplayName("扩展名合法但文件头不符时被拒——挡住改名伪装")
    void rejectsMismatchedMagicBytes() {
        MultipartFile disguised = new MockMultipartFile(
                "file", "payload.png", "image/png", "<% out.print(1); %>".getBytes());

        assertThatThrownBy(() -> fileService.upload(disguised))
                .isInstanceOf(BizException.class)
                .hasMessage("文件内容与扩展名 .png 不符");
    }

    @Test
    @DisplayName("上传成功时用雪花 ID 重命名，原始文件名不进存储路径")
    void renamesFileAndIgnoresOriginalName() throws Exception {
        FileUploadVO vo = fileService.upload(png("../../etc/passwd.png", 64));

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());

        assertThat(captor.getValue().bucket()).isEqualTo("monolith");
        assertThat(captor.getValue().object())
                .matches("\\d{4}/\\d{2}/\\d{2}/\\d+\\.png")
                .doesNotContain("..");

        assertThat(vo.getObjectKey()).isEqualTo(captor.getValue().object());
        assertThat(vo.getOriginalName()).isEqualTo("../../etc/passwd.png");
        assertThat(vo.getSize()).isEqualTo(64);
    }

    @Test
    @DisplayName("contentType 由服务端按扩展名判定，不采信客户端")
    void ignoresClientSuppliedContentType() {
        MultipartFile lying = new MockMultipartFile("file", "a.png", "text/html", pngBytes(64));

        FileUploadVO vo = fileService.upload(lying);

        assertThat(vo.getContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("publicUrl 配置时用它拼 URL，而不是 endpoint")
    void usesPublicUrlWhenConfigured() {
        properties.setPublicUrl("https://cdn.example.com/");

        FileUploadVO vo = fileService.upload(png("a.png", 64));

        assertThat(vo.getUrl())
                .startsWith("https://cdn.example.com/monolith/")
                .endsWith(".png")
                .doesNotContain("127.0.0.1");
    }

    @Test
    @DisplayName("未配 publicUrl 时回退到 endpoint")
    void fallsBackToEndpoint() {
        FileUploadVO vo = fileService.upload(png("a.png", 64));

        assertThat(vo.getUrl()).startsWith("http://127.0.0.1:9000/monolith/");
    }

    @Test
    @DisplayName("对象存储抛异常时转成业务异常，不把底层栈暴露出去")
    void wrapsStorageFailure() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> fileService.upload(png("a.png", 64)))
                .isInstanceOf(BizException.class)
                .hasMessage("对象存储不可用，请稍后重试");
    }

    private MultipartFile png(String filename, int size) {
        return new MockMultipartFile("file", filename, "image/png", pngBytes(size));
    }

    private static byte[] pngBytes(int size) {
        byte[] content = new byte[size];
        System.arraycopy(PNG_MAGIC, 0, content, 0, PNG_MAGIC.length);
        return content;
    }
}
