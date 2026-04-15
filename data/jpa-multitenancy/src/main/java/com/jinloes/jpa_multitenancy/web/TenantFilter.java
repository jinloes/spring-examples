package com.jinloes.jpa_multitenancy.web;

import com.jinloes.jpa_multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reads the {@code X-Tenant} header and loads it into {@link TenantContext} for the request. */
@Component
public class TenantFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String tenantId = request.getHeader("X-Tenant");
    if (tenantId != null) {
      TenantContext.setCurrentTenant(tenantId);
    }
    try {
      chain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }
}
