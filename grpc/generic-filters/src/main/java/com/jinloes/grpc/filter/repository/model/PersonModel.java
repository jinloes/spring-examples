package com.jinloes.grpc.filter.repository.model;

import java.util.List;

/**
 * Canonical domain model for a person. Both repository backends normalize their internal
 * representation to this record before returning data to the service layer.
 */
public record PersonModel(
    String id,
    String name,
    int age,
    String department,
    double salary,
    boolean active,
    AddressModel address,
    ContactModel contact,
    List<String> tags) {}
