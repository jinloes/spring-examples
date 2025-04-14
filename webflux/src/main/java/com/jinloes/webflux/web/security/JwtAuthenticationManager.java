package com.jinloes.webflux.web.security;

import com.jinloes.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
  private final JwtService jwtService;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    return Mono.just(authentication)
        .cast(JwtToken.class)
        .filter(jwtToken -> jwtService.isTokenValid(jwtToken.getToken()))
        .map(jwtToken -> {
          jwtToken.setAuthenticated(true);
          return (Authentication) jwtToken;
        })
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Token was not valid.")));

  }
}
