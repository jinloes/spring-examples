package com.jinloes.spring_examples.elasticsearch.data;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AlarmRepository extends ElasticsearchRepository<Alarm, String> {}
