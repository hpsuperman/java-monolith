package com.hpsuperman.monolith.modules.demo.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = DemoTopics.DEMO_TOPIC,
        consumerGroup = DemoTopics.DEMO_CONSUMER_GROUP,
        selectorExpression = "*")
public class DemoConsumer implements RocketMQListener<DemoMessage> {
    @Override
    public void onMessage(DemoMessage message) {
        log.info("收到 MQ 消息 | id={} | content={} | sentAt={}",
                 message.getId(), message.getContent(), message.getSentAt());
    }
}
