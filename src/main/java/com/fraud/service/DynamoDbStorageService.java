package com.fraud.service;

import com.fraud.model.FraudResult;
import com.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * DynamoDB存储服务类，负责将交易和欺诈检测结果存储到DynamoDB
 */
@Service
@RequiredArgsConstructor
public class DynamoDbStorageService {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbStorageService.class);

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table.name}")
    private String tableName;

    /**
     * 保存交易和欺诈检测结果到DynamoDB
     */
    public void saveTransactionResult(Transaction transaction, FraudResult result) {
        try {
            // 构建DynamoDB项目属性
            Map<String, AttributeValue> item = new HashMap<>();
            
            // 交易信息
            item.put("transactionId", AttributeValue.builder().s(transaction.getTransactionId()).build());
            item.put("accountId", AttributeValue.builder().s(transaction.getAccountId()).build());
            item.put("amount", AttributeValue.builder().n(String.valueOf(transaction.getAmount())).build());
            item.put("location", AttributeValue.builder().s(transaction.getLocation()).build());
            item.put("merchantId", AttributeValue.builder().s(transaction.getMerchantId()).build());
            
            // 欺诈检测结果
            item.put("isFraudulent", AttributeValue.builder().bool(result.isFraudulent()).build());
            item.put("fraudReasons", AttributeValue.builder().ss(result.getReasons()).build());
            item.put("detectTime", AttributeValue.builder().s(result.getDetectTime().toString()).build());

            // 保存到DynamoDB
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
            
            dynamoDbClient.putItem(request);
            
            logger.debug("已保存交易数据到DynamoDB - 交易ID: {}, 是否欺诈: {}",
                    transaction.getTransactionId(), result.isFraudulent());

        } catch (Exception e) {
            logger.error("保存交易到DynamoDB失败 - 交易ID: {}", transaction.getTransactionId(), e);
            throw new RuntimeException("无法保存交易到DynamoDB", e);
        }
    }
}
