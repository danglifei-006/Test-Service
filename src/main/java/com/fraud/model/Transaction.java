package com.fraud.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Transaction Model, simulate real Account transaction
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String transactionId;    // transaction ID
    private String accountId;        // account ID
    private double amount;           // transaction amount
    private String location;         // transaction location
    private String merchantId;       // transaction ID
    private Date transactionTime; // transaction time
}
