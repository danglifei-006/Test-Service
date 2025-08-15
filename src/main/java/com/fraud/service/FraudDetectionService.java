package com.fraud.service;

import com.fraud.model.FraudResult;
import com.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 欺诈检测服务类，实现基于规则的欺诈检测逻辑
 */
@Service
@RequiredArgsConstructor
public class FraudDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

    // 注入SNS通知服务
    private final SnsNotificationService snsNotificationService;

    // 从配置文件读取规则参数
    @Value("${fraud.rule.amount.threshold}")
    private double amountThreshold;

    @Value("${fraud.rule.suspicious.accounts}")
    private String suspiciousAccountsStr;

    @Value("${fraud.rule.unusual.location.enable}")
    private boolean enableUnusualLocationCheck;

    // 可疑账户列表
    private List<String> suspiciousAccounts;

    // 初始化：解析可疑账户字符串为列表
    @PostConstruct
    public void init() {
        this.suspiciousAccounts = Arrays.stream(suspiciousAccountsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        logger.info("初始化欺诈检测规则: 金额阈值={}, 可疑账户数={}, 异常地点检测={}",
                amountThreshold, suspiciousAccounts.size(), enableUnusualLocationCheck);
    }

    /**
     * 执行欺诈检测
     * @param transaction 待检测的交易
     * @return 欺诈检测结果
     */
    public FraudResult detectFraud(Transaction transaction) {
        List<String> fraudReasons = new ArrayList<>();
        boolean isFraudulent = false;

        // 规则1: 交易金额超过阈值
        if (transaction.getAmount() > amountThreshold) {
            String reason = String.format("交易金额%.2f超过阈值%.2f", 
                    transaction.getAmount(), amountThreshold);
            fraudReasons.add(reason);
            isFraudulent = true;
        }

        // 规则2: 账户在可疑列表中
        if (suspiciousAccounts.contains(transaction.getAccountId())) {
            String reason = String.format("账户%s属于已知可疑账户", transaction.getAccountId());
            fraudReasons.add(reason);
            isFraudulent = true;
        }

        // 规则3: 异常交易地点检测
        if (enableUnusualLocationCheck && isHighRiskLocation(transaction.getLocation())) {
            String reason = String.format("交易地点%s属于高危地区", transaction.getLocation());
            fraudReasons.add(reason);
            isFraudulent = true;
        }

        // 生成检测结果
        FraudResult result = new FraudResult(
                transaction.getTransactionId(),
                isFraudulent,
                fraudReasons,
                Instant.now()
        );

        // 处理欺诈交易: 记录日志并发送通知
        if (isFraudulent) {
            logger.warn("检测到欺诈交易 - ID: {}, 原因: {}", 
                    transaction.getTransactionId(), fraudReasons);
            snsNotificationService.sendFraudAlert(result);
        } else {
            logger.info("交易正常 - ID: {}, 金额: {}", 
                    transaction.getTransactionId(), transaction.getAmount());
        }

        return result;
    }

    /**
     * 判断是否为高危地区
     */
    private boolean isHighRiskLocation(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        
        // 示例高危地区列表，实际应用中可从配置或数据库加载
        List<String> highRiskLocations = Arrays.asList(
                "HighRiskCountry1", "HighRiskCountry2", "SuspiciousRegion", "Unknown"
        );
        
        return highRiskLocations.stream()
                .anyMatch(location::contains);
    }
}
