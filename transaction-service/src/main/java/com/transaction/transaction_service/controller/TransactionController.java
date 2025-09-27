package com.transaction.transaction_service.controller;

import com.transaction.transaction_service.dto.TransactionRequest;
import com.transaction.transaction_service.dto.TransactionResponse;
import com.transaction.transaction_service.entity.TransactionStatus;
import com.transaction.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        log.info("Creating new transaction from account {} to account {}", 
                request.getFromAccountId(), request.getToAccountId());
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        log.info("Retrieving transaction with ID: {}", transactionId);
        TransactionResponse response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccount(@PathVariable Long accountId) {
        log.info("Retrieving transactions for account: {}", accountId);
        List<TransactionResponse> responses = transactionService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/account/{accountId}/paged")
    public ResponseEntity<Page<TransactionResponse>> getTransactionsByAccountPaged(
            @PathVariable Long accountId, 
            Pageable pageable) {
        log.info("Retrieving paged transactions for account: {}", accountId);
        Page<TransactionResponse> responses = transactionService.getTransactionsByAccountId(accountId, pageable);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByStatus(@PathVariable TransactionStatus status) {
        log.info("Retrieving transactions with status: {}", status);
        List<TransactionResponse> responses = transactionService.getTransactionsByStatus(status);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/account/{accountId}/date-range")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByDateRange(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Retrieving transactions for account {} between {} and {}", accountId, startDate, endDate);
        List<TransactionResponse> responses = transactionService.getTransactionsByDateRange(accountId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }
    
    @PutMapping("/{transactionId}/cancel")
    public ResponseEntity<TransactionResponse> cancelTransaction(@PathVariable String transactionId) {
        log.info("Cancelling transaction with ID: {}", transactionId);
        TransactionResponse response = transactionService.cancelTransaction(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Transaction Service is running");
    }
}
