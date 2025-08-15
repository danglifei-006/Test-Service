package com.fraud.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 交易数据模型类，表示金融交易信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String transactionId;    // 交易唯一标识
    private String accountId;        // 账户ID
    private double amount;           // 交易金额
    private String location;         // 交易地点
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Instant timestamp;       // 交易时间戳(UTC)
    private String merchantId;       // 商户ID
}
