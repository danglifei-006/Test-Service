package com.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.model.Transaction;
import com.fraud.model.FraudResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import java.util.List;

/**
 * SQS消息消费服务类，从SQS队列接收交易消息并处理
 */
@Service
@RequiredArgsConstructor
public class SqsConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(SqsConsumerService.class);

    // 注入依赖
    private final software.amazon.awssdk.services.sqs.SqsClient sqsClient;
    private final FraudDetectionService fraudDetectionService;
    private final DynamoDbStorageService dynamoDbStorageService;
    private final ObjectMapper objectMapper;

    // 配置参数
    @Value("${aws.sqs.queue.url}")
    private String sqsQueueUrl;

    @Value("${aws.sqs.max.messages}")
    private int maxMessages;

    /**
     * 定时从SQS队列接收消息并处理
     */
    @Scheduled(fixedRateString = "${aws.sqs.polling.rate}")
    public void consumeTransactions() {
        try {
            // 创建接收消息请求
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(20)  // 长轮询减少空请求
                    .build();

            // 接收消息
            List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();
            
            if (messages.isEmpty()) {
                logger.trace("SQS队列中没有新消息");
                return;
            }
            
            logger.info("从SQS接收了 {} 条交易消息", messages.size());

            // 处理每条消息
            for (Message message : messages) {
                try {
                    // 解析消息为Transaction对象
                    Transaction transaction = objectMapper.readValue(message.body(), Transaction.class);
                    logger.debug("处理交易 - ID: {}, 账户: {}, 金额: {}",
                            transaction.getTransactionId(), 
                            transaction.getAccountId(), 
                            transaction.getAmount());

                    // 执行欺诈检测
                    FraudResult result = fraudDetectionService.detectFraud(transaction);

                    // 存储交易和检测结果
                    dynamoDbStorageService.saveTransactionResult(transaction, result);

                    // 删除已处理的消息
                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                            .queueUrl(sqsQueueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build();
                    sqsClient.deleteMessage(deleteRequest);
                    
                    logger.debug("已处理并删除消息 - 交易ID: {}", transaction.getTransactionId());

                } catch (Exception e) {
                    // 处理消息失败时不删除，以便重试
                    logger.error("处理消息失败 (消息ID: {})，将在可见性超时后重试", 
                            message.messageId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("SQS消息处理过程中发生错误", e);
        }
    }
}
