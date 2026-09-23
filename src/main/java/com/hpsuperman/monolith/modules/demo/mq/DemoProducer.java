package com.hpsuperman.monolith.modules.demo.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoProducer {
    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    public boolean send(DemoMessage message) {
        return doSend(DemoTopics.DEMO_TOPIC, message);
    }

    public boolean sendWithTag(DemoMessage message, String tag) {
        return doSend(DemoTopics.DEMO_TOPIC + ":" + tag, message);
    }

    private boolean doSend(String destination, DemoMessage message) {
        RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
        if (template == null) {
            log.warn("RocketMQTemplate 不可用（可能未配置 rocketmq.name-server），跳过发送 | destination={} | id={}",
                     destination, message.getId());
            return false;
        }

        try {
            template.convertAndSend(destination, message);
            log.info("MQ 消息发送成功 | destination={} | id={}", destination, message.getId());
            return true;
        } catch (Exception e) {
            log.error("MQ 消息发送失败 | destination={} | id={}", destination, message.getId(), e);
            return false;
        }
    }
}
