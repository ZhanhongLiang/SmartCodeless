package com.robotlive.smartcodeless.build;

import com.rabbitmq.client.Channel;
import com.robotlive.smartcodeless.build.dto.BuildTaskMessage;
import com.robotlive.smartcodeless.config.BuildRabbitConfig;
import com.robotlive.smartcodeless.service.BuildTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class BuildMessageConsumer {

    @Resource
    private BuildTaskService buildTaskService;

    @RabbitListener(queues = BuildRabbitConfig.BUILD_QUEUE, ackMode = "MANUAL")
    public void onBuildMessage(BuildTaskMessage buildTaskMessage, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        if (buildTaskMessage == null || buildTaskMessage.getTaskId() == null) {
            channel.basicReject(deliveryTag, false);
            return;
        }
        try {
            boolean success = buildTaskService.executeBuildTask(buildTaskMessage.getTaskId());
            if (success) {
                channel.basicAck(deliveryTag, false);
            } else {
                channel.basicReject(deliveryTag, false);
            }
        } catch (Exception e) {
            log.error("Build message consumer failed: {}", buildTaskMessage, e);
            channel.basicReject(deliveryTag, false);
        }
    }
}
