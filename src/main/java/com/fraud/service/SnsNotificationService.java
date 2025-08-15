package com.fraud.service;

import com.fraud.model.FraudResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;


@Service
@RequiredArgsConstructor
public class SnsNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SnsNotificationService.class);

    @Autowired
    private final SnsClient snsClient;

    @Value("${aws.sns.topic.arn}")
    private String snsTopicArn;

    /**
     * Send Fraud Alarm to SNS
     */
    public void sendFraudAlert(FraudResult result) {
        try {
            // build msg Content
            String subject = "Fraud Alarm Transaction-ID: " + result.getTransactionId();
            StringBuilder message = new StringBuilder();
            message.append("Detect Fraud Transaction:\n");
            message.append("Transaction-ID: ").append(result.getTransactionId()).append("\n");
            message.append("Detection time: ").append(result.getDetectTime()).append("\n");
            message.append(":\n");
            
            for (String reason : result.getReasons()) {
                message.append("- ").append(reason).append("\n");
            }

            PublishRequest request = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .subject(subject)
                    .message(message.toString())
                    .build();
            
            snsClient.publish(request);
            logger.info("send alarm to SNS success, Transaction ID: {}", result.getTransactionId());
        } catch (Exception e) {
            logger.error("send alarm to SNS failed, Transaction ID: {}", result.getTransactionId(), e);
        }
    }
}
