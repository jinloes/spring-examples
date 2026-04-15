package com.jinloes.jpa_multitenancy.data;

import com.jinloes.jpa_multitenancy.TenantContext;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;

@Slf4j
public class TenantIdentifierResolver
    implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {
  @Override
  public String resolveCurrentTenantIdentifier() {
    String tenant = TenantContext.getCurrentTenant().orElse("public");
    log.debug("Resolved tenant identifier: {}", tenant);
    return tenant;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
  }
}
