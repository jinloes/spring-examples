package com.jinloes.salesforce.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SalesforceProperties.class)
public class SalesforceConfig {

  @Bean
  public RestClient restClient() {
    return RestClient.create();
  }
}
