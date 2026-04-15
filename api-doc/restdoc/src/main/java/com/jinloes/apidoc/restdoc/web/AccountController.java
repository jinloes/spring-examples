package com.jinloes.apidoc.restdoc.web;

import com.jinloes.apidoc.restdoc.model.BankAccount;
import com.jinloes.apidoc.restdoc.model.CreateAccountRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * No OpenAPI annotations here — the API contract is documented entirely through REST Docs tests.
 * Run {@code ./gradlew :api-doc:restdoc:openapi3} to generate {@code build/api-spec/openapi.yaml}.
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

  @GetMapping("/{id}")
  public BankAccount getAccount(@PathVariable UUID id) {
    return new BankAccount(id, "Sample Account", 1000);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BankAccount createAccount(@RequestBody CreateAccountRequest request) {
    return new BankAccount(UUID.randomUUID(), request.name(), request.initialAmount());
  }
}
