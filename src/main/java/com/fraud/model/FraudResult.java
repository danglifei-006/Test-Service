package com.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Fraud Detection Result Model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudResult {
    private String transactionId;    // Transaction-ID
    private boolean isFraudulent;    // isFraud transaction
    private List<String> reasons;    // reason list
    private Date detectTime;      // detection time
}
