package com.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.model.Transaction;
import com.fraud.model.FraudResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import java.util.List;

/**
 * SQS Service, consume transaction msg
 */
@Service
@RequiredArgsConstructor
public class SqsConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(SqsConsumerService.class);


    @Autowired
    private final software.amazon.awssdk.services.sqs.SqsClient sqsClient;
    @Autowired
    private final FraudDetectionService fraudDetectionService;
    @Autowired
    private final SnsNotificationService snsNotificationService;
    @Autowired
    private final ObjectMapper objectMapper;


    @Value("${aws.sqs.queue.url}")
    private String sqsQueueUrl;

    @Value("${aws.sqs.max.messages}")
    private int maxMessages;

    /**
     *  Schedule to Receive msg from SQS
     */
    @Scheduled(fixedRateString = "${aws.sqs.polling.rate}")
    public void consumeTransactions() {
        try {

            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(20)  // 长轮询减少空请求
                    .build();

            List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();
            
            if (messages.isEmpty()) {
                logger.trace("SQS is empty now");
                return;
            }
            logger.info("receive  {}  msg form SQS", messages.size());

            for (Message message : messages) {
                try {
                    Transaction transaction = objectMapper.readValue(message.body(), Transaction.class);
                    logger.debug("Deal Msg - ID: {}, Account: {}, Amount: {}",
                            transaction.getTransactionId(), 
                            transaction.getAccountId(), 
                            transaction.getAmount());


                    FraudResult result = fraudDetectionService.detectFraud(transaction);

                    //TODO persist result to db or obs

                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                            .queueUrl(sqsQueueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build();
                    sqsClient.deleteMessage(deleteRequest);

                    snsNotificationService.sendFraudAlert(result);
                    logger.debug("Transaction Msg - ID: {}", transaction.getTransactionId());

                } catch (Exception e) {
                    // retry
                    logger.error("Deal Msg Failed ID: {})，will retry later",
                            message.messageId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Deal SQS message error for:", e);
        }
    }
}
