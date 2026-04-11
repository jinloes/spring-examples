package com.jinloes.jpa_multitenancy;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {
  private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

  public static void setCurrentTenant(String tenant) {
    log.debug("Setting tenant to {}", tenant);
    currentTenant.set(tenant);
  }

  public static Optional<String> getCurrentTenant() {
    return Optional.ofNullable(currentTenant.get());
  }

  public static void clear() {
    currentTenant.remove();
  }
}
