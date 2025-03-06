package com.jinloes.openapi.web;

import com.jinloes.openapi.web.api.AccountsApi;
import com.jinloes.openapi.web.model.BankAccount;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController implements AccountsApi {
  @Override
  public ResponseEntity<BankAccount> accountsAccountIdGet(UUID accountId) {
    BankAccount account = new BankAccount()
        .id(accountId)
        .name("Sample Bank Account")
        .amount(1000);
    return ResponseEntity.ok(account);
  }
}
