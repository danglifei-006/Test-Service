package com.fraud.service;

import com.fraud.model.FraudResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * SNS通知服务类，当检测到欺诈交易时发送通知
 */
@Service
@RequiredArgsConstructor
public class SnsNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SnsNotificationService.class);

    private final SnsClient snsClient;

    @Value("${aws.sns.topic.arn}")
    private String snsTopicArn;

    /**
     * 发送欺诈检测警报到SNS主题
     */
    public void sendFraudAlert(FraudResult result) {
        try {
            // 构建通知内容
            String subject = "欺诈交易警报 - 交易ID: " + result.getTransactionId();
            StringBuilder message = new StringBuilder();
            message.append("检测到欺诈交易:\n");
            message.append("交易ID: ").append(result.getTransactionId()).append("\n");
            message.append("检测时间: ").append(result.getDetectTime()).append("\n");
            message.append("欺诈原因:\n");
            
            for (String reason : result.getReasons()) {
                message.append("- ").append(reason).append("\n");
            }

            // 发送SNS消息
            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .subject(subject)
                    .message(message.toString())
                    .build();
            
            snsClient.publish(request);
            
            logger.info("已发送欺诈警报到SNS - 交易ID: {}", result.getTransactionId());

        } catch (Exception e) {
            logger.error("发送SNS欺诈警报失败 - 交易ID: {}", result.getTransactionId(), e);
            // 警报发送失败不影响主流程，仅记录日志
        }
    }
}
