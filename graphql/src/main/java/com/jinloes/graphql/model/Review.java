package com.jinloes.graphql.model;

public record Review(String id, String bookId, int rating, String content) {}
