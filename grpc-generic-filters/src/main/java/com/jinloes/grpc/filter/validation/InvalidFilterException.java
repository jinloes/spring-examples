package com.jinloes.grpc.filter.validation;

/** Thrown when a {@code FilterRequest} references an unknown field or a disallowed operator. */
public class InvalidFilterException extends RuntimeException {

  public InvalidFilterException(String message) {
    super(message);
  }
}
