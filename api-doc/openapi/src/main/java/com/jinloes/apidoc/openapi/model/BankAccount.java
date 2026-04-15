package com.jinloes.apidoc.openapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "A bank account")
public record BankAccount(
    @Schema(description = "Account UUID") UUID id,
    @Schema(description = "Account holder name") String name,
    @Schema(description = "Current balance in cents") int amount) {}
