package com.hpsuperman.monolith.modules.demo.controller;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.hpsuperman.monolith.common.result.Result;
import com.hpsuperman.monolith.modules.demo.mq.DemoMessage;
import com.hpsuperman.monolith.modules.demo.mq.DemoProducer;
import com.hpsuperman.monolith.modules.demo.mq.DemoTopics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "MQ 示例")
@Validated
@RestController
@RequestMapping("/api/demo/mq")
@RequiredArgsConstructor
public class DemoMqController {
    private final DemoProducer demoProducer;

    @Operation(summary = "发送一条测试消息", description = "返回 false 表示 broker 不可用或发送失败")
    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> send(@RequestParam @NotBlank(message = "content 不能为空") String content) {
        DemoMessage message = new DemoMessage(IdWorker.getId(), content, LocalDateTime.now());
        return Result.success(demoProducer.send(message));
    }

    @Operation(summary = "发送带 tag 的测试消息")
    @PostMapping("/send-with-tag")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> sendWithTag(@RequestParam @NotBlank String content) {
        DemoMessage message = new DemoMessage(IdWorker.getId(), content, LocalDateTime.now());
        return Result.success(demoProducer.sendWithTag(message, DemoTopics.TAG_CREATED));
    }
}
