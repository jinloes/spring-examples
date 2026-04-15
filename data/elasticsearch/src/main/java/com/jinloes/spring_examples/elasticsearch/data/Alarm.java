package com.jinloes.spring_examples.elasticsearch.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "alarm")
public record Alarm(@Id String id, int org) {}
