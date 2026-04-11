package com.jinloes.grpc.filter.service;

import com.jinloes.grpc.filter.engine.FilterEngine;
import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.Person;
import com.jinloes.grpc.filter.proto.PersonFilterServiceGrpc;
import com.jinloes.grpc.filter.proto.PersonsResponse;
import com.jinloes.grpc.filter.repository.PersonRepository;
import com.jinloes.grpc.filter.validation.FilterRequestValidator;
import com.jinloes.grpc.filter.validation.InvalidFilterException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PersonFilterServiceImpl extends PersonFilterServiceGrpc.PersonFilterServiceImplBase {

  private final PersonRepository repository;
  private final FilterEngine filterEngine;
  private final FilterRequestValidator validator;

  @Override
  public void listPersons(FilterRequest request, StreamObserver<PersonsResponse> responseObserver) {
    log.info(
        "ListPersons: {} filter(s), operator={}",
        request.getFiltersCount(),
        request.getLogicalOperator());

    try {
      validator.validate(request);

      List<Person> all = repository.findAll();
      List<Person> matched = filterEngine.apply(all, request);

      PersonsResponse response =
          PersonsResponse.newBuilder()
              .addAllPersons(matched)
              .setTotalMatched(matched.size())
              .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (InvalidFilterException e) {
      // Operator not allowed for field, or field is not filterable
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (IllegalArgumentException e) {
      // Unknown field path encountered during filter engine traversal
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (UnsupportedOperationException e) {
      responseObserver.onError(
          Status.UNIMPLEMENTED.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
