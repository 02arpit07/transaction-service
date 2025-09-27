package com.transaction.transaction_service.service;

import com.transaction.transaction_service.client.AccountServiceClient;
import com.transaction.transaction_service.dto.AccountInfo;
import com.transaction.transaction_service.dto.TransactionRequest;
import com.transaction.transaction_service.dto.TransactionResponse;
import com.transaction.transaction_service.entity.Transaction;
import com.transaction.transaction_service.entity.TransactionStatus;
import com.transaction.transaction_service.exception.TransactionException;
import com.transaction.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        log.info("Creating transaction from account {} to account {} with amount {}", 
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        
        // Validate accounts exist and get account information
        AccountInfo fromAccount = validateAndGetAccount(request.getFromAccountId());
        AccountInfo toAccount = validateAndGetAccount(request.getToAccountId());
        
        // Validate sufficient balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new TransactionException("Insufficient balance in source account");
        }
        
        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setFromAccountId(request.getFromAccountId());
        transaction.setToAccountId(request.getToAccountId());
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setDescription(request.getDescription());
        
        try {
            // Update account balances
            updateAccountBalances(request.getFromAccountId(), request.getToAccountId(), request.getAmount());
            
            // Mark transaction as completed
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction = transactionRepository.save(transaction);
            
            log.info("Transaction {} completed successfully", transaction.getTransactionId());
            return mapToResponse(transaction);
            
        } catch (Exception e) {
            log.error("Error processing transaction: {}", e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new TransactionException("Transaction failed: " + e.getMessage());
        }
    }
    
    public TransactionResponse getTransactionById(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found with ID: " + transactionId));
        return mapToResponse(transaction);
    }
    
    public List<TransactionResponse> getTransactionsByAccountId(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public Page<TransactionResponse> getTransactionsByAccountId(Long accountId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, pageable);
        return transactions.map(this::mapToResponse);
    }
    
    public List<TransactionResponse> getTransactionsByStatus(TransactionStatus status) {
        List<Transaction> transactions = transactionRepository.findByStatus(status);
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public List<TransactionResponse> getTransactionsByDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TransactionResponse cancelTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found with ID: " + transactionId));
        
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            throw new TransactionException("Only completed transactions can be cancelled");
        }
        
        try {
            // Reverse the transaction
            updateAccountBalances(transaction.getToAccountId(), transaction.getFromAccountId(), transaction.getAmount());
            transaction.setStatus(TransactionStatus.CANCELLED);
            transaction = transactionRepository.save(transaction);
            
            log.info("Transaction {} cancelled successfully", transactionId);
            return mapToResponse(transaction);
            
        } catch (Exception e) {
            log.error("Error cancelling transaction: {}", e.getMessage());
            throw new TransactionException("Failed to cancel transaction: " + e.getMessage());
        }
    }
    
    private AccountInfo validateAndGetAccount(Long accountId) {
        try {
            ResponseEntity<AccountInfo> response = accountServiceClient.getAccount(accountId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new TransactionException("Account not found: " + accountId);
            }
        } catch (Exception e) {
            log.error("Error validating account {}: {}", accountId, e.getMessage());
            throw new TransactionException("Failed to validate account: " + accountId);
        }
    }
    
    private void updateAccountBalances(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        try {
            // Get current balances
            ResponseEntity<BigDecimal> fromBalanceResponse = accountServiceClient.getBalance(fromAccountId);
            ResponseEntity<BigDecimal> toBalanceResponse = accountServiceClient.getBalance(toAccountId);
            
            if (!fromBalanceResponse.getStatusCode().is2xxSuccessful() || 
                !toBalanceResponse.getStatusCode().is2xxSuccessful()) {
                throw new TransactionException("Failed to get account balances");
            }
            
            BigDecimal fromBalance = fromBalanceResponse.getBody();
            BigDecimal toBalance = toBalanceResponse.getBody();
            
            // Update balances
            accountServiceClient.updateBalance(fromAccountId, fromBalance.subtract(amount));
            accountServiceClient.updateBalance(toAccountId, toBalance.add(amount));
            
        } catch (Exception e) {
            log.error("Error updating account balances: {}", e.getMessage());
            throw new TransactionException("Failed to update account balances");
        }
    }
    
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
