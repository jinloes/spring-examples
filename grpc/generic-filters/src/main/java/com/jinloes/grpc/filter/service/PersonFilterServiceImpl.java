package com.jinloes.grpc.filter.service;

import com.jinloes.grpc.filter.proto.Backend;
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
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Qualifier;

@GrpcService
@Slf4j
public class PersonFilterServiceImpl extends PersonFilterServiceGrpc.PersonFilterServiceImplBase {

  private final Map<Backend, PersonRepository> repositories;
  private final FilterRequestValidator validator;

  public PersonFilterServiceImpl(
      @Qualifier("inMemory") PersonRepository inMemory,
      @Qualifier("alternative") PersonRepository alternative,
      FilterRequestValidator validator) {
    this.repositories =
        Map.of(
            Backend.IN_MEMORY, inMemory,
            Backend.ALTERNATIVE, alternative);
    this.validator = validator;
  }

  @Override
  public void listPersons(FilterRequest request, StreamObserver<PersonsResponse> responseObserver) {
    log.info(
        "ListPersons: {} filter(s), operator={}, backend={}",
        request.getFiltersCount(),
        request.getLogicalOperator(),
        request.getBackend());

    try {
      validator.validate(request);

      PersonRepository repository =
          repositories.getOrDefault(request.getBackend(), repositories.get(Backend.IN_MEMORY));
      List<Person> matched = repository.filter(request);

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
      // Unknown field path encountered during filtering
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    } catch (UnsupportedOperationException e) {
      responseObserver.onError(
          Status.UNIMPLEMENTED.withDescription(e.getMessage()).asRuntimeException());
    }
  }
}
