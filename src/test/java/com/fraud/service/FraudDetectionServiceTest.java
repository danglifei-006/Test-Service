package com.fraud.service;

import com.fraud.model.FraudResult;
import com.fraud.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class FraudDetectionServiceTest {

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private Transaction highAmountTx;    // high  amount
    private Transaction suspiciousAcctTx; // suspicious account
    private Transaction normalTx;        // normal transaction
    private Transaction highRiskLocationTx; // high region region

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(fraudDetectionService, "amountThreshold", 10000.0);
        ReflectionTestUtils.setField(fraudDetectionService, "suspiciousAccountsStr", "ACCT-123,ACCT-456");
        ReflectionTestUtils.setField(fraudDetectionService, "enableUnusualLocationCheck", true);
        ReflectionTestUtils.setField(fraudDetectionService, "highRiskLocationsStr", "HighRiskCountry1, HighRiskCountry2, SuspiciousRegion, Unknown");

        fraudDetectionService.init();

        // create test data
        Instant now = Instant.now();
        highAmountTx = new Transaction("TX-001", "ACCT-789", 15000.0, "New York",  "MCH-001", Date.valueOf(LocalDate.now()));
        suspiciousAcctTx = new Transaction("TX-002", "ACCT-123", 5000.0, "London",  "MCH-002", Date.valueOf(LocalDate.now()));
        normalTx = new Transaction("TX-003", "ACCT-789", 8000.0, "Paris",  "MCH-003", Date.valueOf(LocalDate.now()));
        highRiskLocationTx = new Transaction("TX-004", "ACCT-789", 8000.0, "HighRiskCountry1",  "MCH-004", Date.valueOf(LocalDate.now()));
    }

    @Test
    void testHighAmountTransaction_ShouldBeFraud() {
        FraudResult result = fraudDetectionService.detectFraud(highAmountTx);
        assertTrue(result.isFraudulent());
        assertEquals(1, result.getReasons().size());
        assertTrue(result.getReasons().get(0).contains("exceeds the threshold of 10000.0"));
    }

    @Test
    void testSuspiciousAccountTransaction_ShouldBeFraud() {
        FraudResult result = fraudDetectionService.detectFraud(suspiciousAcctTx);
        assertTrue(result.isFraudulent());
        assertEquals(1, result.getReasons().size());
        assertTrue(result.getReasons().get(0).contains("known suspicious account"));
    }

    @Test
    void testNormalTransaction_ShouldNotBeFraud() {
        FraudResult result = fraudDetectionService.detectFraud(normalTx);
        assertFalse(result.isFraudulent());
        assertTrue(result.getReasons().isEmpty());
    }

    @Test
    void testHighRiskLocationTransaction_ShouldBeFraud() {
        FraudResult result = fraudDetectionService.detectFraud(highRiskLocationTx);
        assertTrue(result.isFraudulent());
        assertEquals(1, result.getReasons().size());
        assertTrue(result.getReasons().get(0).contains("high risk area"));
    }

    @Test
    void testMultipleRulesTriggered_ShouldBeFraud() {
        Transaction tx = new Transaction("TX-005", "ACCT-123", 15000.0, "HighRiskCountry1",  "MCH-005",  Date.valueOf(LocalDate.now()));
        FraudResult result = fraudDetectionService.detectFraud(tx);
        assertTrue(result.isFraudulent());
        assertEquals(3, result.getReasons().size());
    }
}
