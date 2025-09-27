package com.transaction.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo {
    private Long accountId;
    private BigDecimal balance;
    private String accountType;
    private String status;
}
