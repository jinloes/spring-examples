package com.jinloes.graphql.model;

/**
 * Internal book model. {@code authorId} is a foreign-key reference; it is not exposed in the
 * GraphQL schema — the {@code author} field is resolved separately via {@code @BatchMapping}.
 */
public record Book(String id, String title, String authorId) {}
