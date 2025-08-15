package com.fraud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.model.FraudResult;
import com.fraud.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerServiceTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private SnsNotificationService snsNotificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SqsConsumerService sqsConsumerService;

    @Captor
    private ArgumentCaptor<ReceiveMessageRequest> receiveMessageRequestCaptor;

    @Captor
    private ArgumentCaptor<DeleteMessageRequest> deleteMessageRequestCaptor;


    private final String testQueueUrl = "https://sqs.test-region.amazonaws.com/123456/test-queue.fifo";
    private final int testMaxMessages = 10;
    private final Transaction testTransaction = new Transaction("TEST-12345", "ACCT-123", 15000.0, "HighRiskCountry1", "MCH-TEST", Date.valueOf(LocalDate.now()));
    private final FraudResult testFraudResult = new FraudResult("TEST-12345", true,
            Collections.singletonList("Amount exceeds threshold"), new java.sql.Date(System.currentTimeMillis()));

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(sqsConsumerService, "sqsQueueUrl", testQueueUrl);
        ReflectionTestUtils.setField(sqsConsumerService, "maxMessages", testMaxMessages);

    }

    /**
     * case1：sqs is empty
     */
    @Test
    void consumeTransactions_WhenNoMessagesInSQS_NoProcessing() {

        ReceiveMessageResponse emptyResponse = ReceiveMessageResponse.builder().messages(Collections.emptyList()).build();
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(emptyResponse);


        sqsConsumerService.consumeTransactions();


        verify(sqsClient).receiveMessage(receiveMessageRequestCaptor.capture());
        ReceiveMessageRequest capturedRequest = receiveMessageRequestCaptor.getValue();
        assertEquals(testQueueUrl, capturedRequest.queueUrl());
        assertEquals(testMaxMessages, capturedRequest.maxNumberOfMessages());
  
        verify(fraudDetectionService, never()).detectFraud(any(Transaction.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(snsNotificationService, never()).sendFraudAlert(any(FraudResult.class));
    }

    /**
     * case2：SQS get msg
     */
    @Test
    void consumeTransactions_WhenMessagesExistInSQS_ProcessesSuccessfully() throws JsonProcessingException {
        // 1. 准备测试消息
        String testMessageBody = "{\"transactionId\":\"TXN123\",\"accountId\":\"ACC456\",\"amount\":1500.0,\"location\":\"HighRiskArea\"}";
        Message testMessage = Message.builder()
                .messageId("MSG789")
                .body(testMessageBody)
                .receiptHandle("RECEIPT123")
                .build();
        ReceiveMessageResponse response = ReceiveMessageResponse.builder().messages(List.of(testMessage)).build();

        // 2. simulate
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);
        when(objectMapper.readValue(testMessageBody, Transaction.class)).thenReturn(testTransaction);
        when(fraudDetectionService.detectFraud(testTransaction)).thenReturn(testFraudResult);


        sqsConsumerService.consumeTransactions();


        verify(objectMapper).readValue(testMessageBody, Transaction.class);

        verify(fraudDetectionService).detectFraud(testTransaction);

        verify(sqsClient).deleteMessage(deleteMessageRequestCaptor.capture());
        DeleteMessageRequest deleteRequest = deleteMessageRequestCaptor.getValue();
        assertEquals(testQueueUrl, deleteRequest.queueUrl());
        assertEquals("RECEIPT123", deleteRequest.receiptHandle());

        verify(snsNotificationService).sendFraudAlert(testFraudResult);
    }

    /**
     *  case 3 parse failed
     */
    @Test
    void consumeTransactions_WhenMessageParsingFails_DoesNotDeleteOrNotify() throws JsonProcessingException {
        // 1. 准备测试消息
        Message testMessage = Message.builder()
                .messageId("MSG789")
                .body("invalid-json")
                .receiptHandle("RECEIPT123")
                .build();
        ReceiveMessageResponse response = ReceiveMessageResponse.builder().messages(List.of(testMessage)).build();


        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(response);
        when(objectMapper.readValue("invalid-json", Transaction.class)).thenThrow(JsonProcessingException.class);


        sqsConsumerService.consumeTransactions();


        verify(fraudDetectionService, never()).detectFraud(any(Transaction.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
        verify(snsNotificationService, never()).sendFraudAlert(any(FraudResult.class));
    }

    /**
     * case : throw exception when receive msg
     */
    @Test
    void consumeTransactions_WhenSqsClientThrowsException_HandlesGracefully() {

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(SqsException.class);


        sqsConsumerService.consumeTransactions();


        verify(fraudDetectionService, never()).detectFraud(any(Transaction.class));
        verify(snsNotificationService, never()).sendFraudAlert(any(FraudResult.class));
    }
}