package com.jinloes.grpc.ui.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GrpcExceptionHandler {

  // Translates gRPC StatusRuntimeException to HTTP responses so REST clients
  // receive meaningful status codes rather than a 500 for every gRPC error.
  @ExceptionHandler(StatusRuntimeException.class)
  public ResponseEntity<String> handle(StatusRuntimeException ex) {
    HttpStatus httpStatus =
        switch (ex.getStatus().getCode()) {
          case NOT_FOUND -> HttpStatus.NOT_FOUND;
          case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
          case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
          case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
          default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    // Prefer the gRPC status description (set explicitly in the service) over the
    // raw status code name, which is less readable.
    String message =
        ex.getStatus().getDescription() != null
            ? ex.getStatus().getDescription()
            : Status.Code.values()[ex.getStatus().getCode().value()].name();
    return ResponseEntity.status(httpStatus).body(message);
  }
}
