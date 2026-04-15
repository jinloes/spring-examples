package com.jinloes.webflux.web.security;

import com.jinloes.jwt.JwtService;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {
  private static final String BEARER = "Bearer ";

  private final JwtService jwtService;

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    return Mono.justOrEmpty(
            exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION)
        )
        .filter(header -> header.startsWith(BEARER))
        .map(header -> header.substring(BEARER.length()))
        .map(token -> new JwtToken(token, createUserDetails(token)));
  }

  private UserDetails createUserDetails(String token) {
    final Claims claims = jwtService.getClaims(token);
    String tenantId = Objects.toString(claims.get("tenantId"), null);

    return User.builder()
        .username(tenantId)
        .password("")
        .authorities(List.of())
        .build();
  }
}
