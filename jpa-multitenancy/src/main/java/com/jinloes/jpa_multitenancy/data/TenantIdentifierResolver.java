package com.jinloes.jpa_multitenancy.data;

import com.jinloes.jpa_multitenancy.TenantContext;
import java.util.Map;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String>,
    HibernatePropertiesCustomizer {
  @Override
  public String resolveCurrentTenantIdentifier() {
    return TenantContext.getCurrentTenant()
        .orElse("public");
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }

  @Override public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
  }
}
