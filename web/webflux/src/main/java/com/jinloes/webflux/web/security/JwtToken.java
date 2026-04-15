package com.jinloes.webflux.web.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class JwtToken extends AbstractAuthenticationToken {
  private final String token;
  private final UserDetails principal;

  public JwtToken(String token, UserDetails principal) {
    super(principal.getAuthorities());
    this.token = token;
    this.principal = principal;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }
}
