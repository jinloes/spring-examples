package com.jinloes.jpa_multitenancy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  /*@Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
      MultiTenantConnectionProvider multiTenantConnectionProvider,
      CurrentTenantIdentifierResolver tenantIdentifierResolver) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();

    em.setDataSource(dataSource);
    em.setJpaVendorAdapter(jpaVendorAdapter());
    em.setPackagesToScan("com.*");

    Map<String, Object> jpaProperties = new HashMap<>();
    jpaProperties.put(Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
    jpaProperties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
    jpaProperties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
    jpaProperties.put(Environment.FORMAT_SQL, true);
    em.setJpaPropertyMap(jpaProperties);

    return em;
  }*/
}
