package com.jinloes.apidoc.openapi.web;

import com.jinloes.apidoc.openapi.model.BankAccount;
import com.jinloes.apidoc.openapi.model.CreateAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Demonstrates the annotation-driven approach: Springdoc reads @Operation / @Schema annotations at
 * startup and auto-generates the OpenAPI spec without any test involvement.
 *
 * <p>Access the live docs at:
 *
 * <ul>
 *   <li>JSON spec: http://localhost:8080/v3/api-docs
 *   <li>Swagger UI: http://localhost:8080/swagger-ui.html
 * </ul>
 */
@RestController
@RequestMapping("/accounts")
@Tag(name = "Accounts", description = "Bank account operations")
public class AccountController {

  @GetMapping("/{id}")
  @Operation(
      summary = "Get account by ID",
      responses =
          @ApiResponse(
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = BankAccount.class))))
  public BankAccount getAccount(@Parameter(description = "Account UUID") @PathVariable UUID id) {
    return new BankAccount(id, "Sample Account", 1000);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a new account",
      responses =
          @ApiResponse(
              responseCode = "201",
              content = @Content(schema = @Schema(implementation = BankAccount.class))))
  public BankAccount createAccount(@RequestBody CreateAccountRequest request) {
    return new BankAccount(UUID.randomUUID(), request.name(), request.initialAmount());
  }
}
