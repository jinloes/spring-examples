package com.jinloes.jpa_multitenancy.config;

import com.jinloes.jpa_multitenancy.data.TenantConnectionProvider;
import com.jinloes.jpa_multitenancy.data.TenantIdentifierResolver;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {
  @Bean
  public TenantIdentifierResolver tenantIdentifierResolver() {
    return new TenantIdentifierResolver();
  }

  @Bean
  public TenantConnectionProvider tenantConnectionProvider(DataSource dataSource) {
    return new TenantConnectionProvider(dataSource);
  }
}
