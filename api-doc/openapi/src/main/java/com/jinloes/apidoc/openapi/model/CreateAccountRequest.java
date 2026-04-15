package com.jinloes.apidoc.openapi.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a new bank account")
public record CreateAccountRequest(
    @Schema(description = "Account holder name", example = "Jane Doe") String name,
    @Schema(description = "Initial deposit in cents", example = "50000") int initialAmount) {}
