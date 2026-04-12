package com.jinloes.grpc.filter.repository;

import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.Person;
import java.util.List;

/**
 * Data access contract for person records. Each implementation owns its internal data model and a
 * dedicated {@link com.jinloes.grpc.filter.engine.FilterEngine}, applying filters at the model
 * level before normalizing results to {@link Person} proto.
 */
public interface PersonRepository {

  List<Person> filter(FilterRequest request);
}
