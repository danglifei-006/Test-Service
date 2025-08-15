package com.fraud.service;

import com.fraud.model.FraudResult;
import com.fraud.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fraud Detection Service
 */
@Service
@RequiredArgsConstructor

public class FraudDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

    @Autowired
    private final SnsNotificationService snsNotificationService;

    // fraud max amount
    @Value("${fraud.rule.amount.threshold}")
    private double amountThreshold;
    // fraud suspicious accounts
    @Value("${fraud.rule.suspicious.accounts}")
    private String suspiciousAccountsStr;

    @Value("${fraud.rule.risk.locations}")
    private String highRiskLocationsStr;

    @Value("${fraud.rule.unusual.location.enable}")
    private boolean enableUnusualLocationCheck;


    private List<String> suspiciousAccounts;

    private List<String> highRiskLocations;


    @PostConstruct
    public void init() {
        this.suspiciousAccounts = Arrays.stream(suspiciousAccountsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        this.highRiskLocations = Arrays.stream(highRiskLocationsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        logger.info("init fraud rule: amount-threshold={}, number of suspicious accounts\n={}, high risk location detection enabled={}",
                amountThreshold, suspiciousAccounts.size(), enableUnusualLocationCheck);
    }

    /**
     * @param transaction
     * @return check Result
     */
    public FraudResult detectFraud(Transaction transaction) {
        List<String> fraudReasons = new ArrayList<>();
        boolean isFraudulent = false;

        // Rule1:  exceed max amount
        if (transaction.getAmount() > amountThreshold) {
            String reason = String.format("The transaction amount of %.2f exceeds the threshold of %.2f.",
                    transaction.getAmount(), amountThreshold);
            fraudReasons.add(reason);
            isFraudulent = true;
        }

        // Rule2: account in suspicious list
        if (suspiciousAccounts.contains(transaction.getAccountId())) {
            String reason = String.format("Account %s belongs to a known suspicious account.", transaction.getAccountId());
            fraudReasons.add(reason);
            isFraudulent = true;
        }

        // rule3: high risk region
        if (enableUnusualLocationCheck && isHighRiskLocation(transaction.getLocation())) {
            String reason = String.format("The transaction location %s belongs to a high risk area.", transaction.getLocation());
            fraudReasons.add(reason);
            isFraudulent = true;
        }


        FraudResult result = new FraudResult(
                transaction.getTransactionId(),
                isFraudulent,
                fraudReasons,
                Date.valueOf(LocalDate.now())
        );


        if (isFraudulent) {
            logger.warn("Detect Fraud Transaction, ID: {}, reason: {}",
                    transaction.getTransactionId(), fraudReasons);
        } else {
            logger.info("Transaction is normal - ID: {}, amount: {}",
                    transaction.getTransactionId(), transaction.getAmount());
        }
        return result;
    }

    /**
     * is high risk region
     */
    private boolean isHighRiskLocation(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        return highRiskLocations.stream()
                .anyMatch(location::contains);
    }
}
