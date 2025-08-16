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
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class FraudDetectionServiceTest {

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Mock
    private SnsNotificationService snsNotificationService;

    private Transaction highAmountTx;    // 高金额交易
    private Transaction suspiciousAcctTx; // 可疑账户交易
    private Transaction normalTx;        // 正常交易
    private Transaction highRiskLocationTx; // 高危地区交易

    @BeforeEach
    void setUp() {
        // 设置配置参数
        ReflectionTestUtils.setField(fraudDetectionService, "amountThreshold", 10000.0);
        ReflectionTestUtils.setField(fraudDetectionService, "suspiciousAccountsStr", "ACCT-123,ACCT-456");
        ReflectionTestUtils.setField(fraudDetectionService, "enableUnusualLocationCheck", true);

        // 初始化服务
        fraudDetectionService.init();

        // 模拟SNS通知
       // doNothing().when(snsNotificationService).sendFraudAlert(org.mockito.ArgumentMatchers.any(FraudResult.class));

        // 创建测试交易
        Instant now = Instant.now();
        highAmountTx = new Transaction("TX-001", "ACCT-789", 15000.0, "New York",  "MCH-001");
        suspiciousAcctTx = new Transaction("TX-002", "ACCT-123", 5000.0, "London",  "MCH-002");
        normalTx = new Transaction("TX-003", "ACCT-789", 8000.0, "Paris",  "MCH-003");
        highRiskLocationTx = new Transaction("TX-004", "ACCT-789", 8000.0, "HighRiskCountry1",  "MCH-004");
    }

    @Test
    void testHighAmountTransaction_ShouldBeFraud() {
        FraudResult result = fraudDetectionService.detectFraud(highAmountTx);
        assertTrue(result.isFraudulent());
        assertEquals(1, result.getReasons().size());
        assertTrue(result.getReasons().get(0).contains("超过阈值10000.0"));
    }

    @Test
    void testSuspiciousAccountTransaction_ShouldBeFraud() {
        FraudResult result = fraudDetectionService.detectFraud(suspiciousAcctTx);
        assertTrue(result.isFraudulent());
        assertEquals(1, result.getReasons().size());
        assertTrue(result.getReasons().get(0).contains("已知可疑账户"));
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
        assertTrue(result.getReasons().get(0).contains("高危地区"));
    }

    @Test
    void testMultipleRulesTriggered_ShouldBeFraud() {
        Transaction tx = new Transaction("TX-005", "ACCT-123", 15000.0, "HighRiskCountry1",  "MCH-005");
        FraudResult result = fraudDetectionService.detectFraud(tx);
        assertTrue(result.isFraudulent());
        assertEquals(3, result.getReasons().size()); // 金额+可疑账户+高危地区
    }
}
