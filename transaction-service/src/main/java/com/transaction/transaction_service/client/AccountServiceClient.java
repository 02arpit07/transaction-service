package com.transaction.transaction_service.client;

import com.transaction.transaction_service.dto.AccountInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "account-service", url = "${account.service.url:http://localhost:8081}")
public interface AccountServiceClient {
    
    @GetMapping("/api/accounts/{accountId}")
    ResponseEntity<AccountInfo> getAccount(@PathVariable("accountId") Long accountId);
    
    @PutMapping("/api/accounts/{accountId}/balance")
    ResponseEntity<Void> updateBalance(@PathVariable("accountId") Long accountId, 
                                     @RequestBody BigDecimal newBalance);
    
    @GetMapping("/api/accounts/{accountId}/balance")
    ResponseEntity<BigDecimal> getBalance(@PathVariable("accountId") Long accountId);
}
