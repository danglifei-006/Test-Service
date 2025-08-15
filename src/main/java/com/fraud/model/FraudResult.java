package com.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

/**
 * 欺诈检测结果模型类，表示对交易的欺诈检测结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudResult {
    private String transactionId;    // 关联的交易ID
    private boolean isFraudulent;    // 是否为欺诈交易
    private List<String> reasons;    // 欺诈原因列表
    private Instant detectTime;      // 检测时间
}
