package com.jinloes.webflux.config;

import com.jinloes.jwt.JwtService;
import com.jinloes.webflux.web.security.JwtAuthenticationManager;
import com.jinloes.webflux.web.security.JwtServerAuthenticationConverter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public KeyPair testKeyPair() {
    KeyPairGenerator keyPairGenerator;
    try {
      keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }

  @Bean
  public JwtService jwtService(KeyPair testKeyPair) {
    return new JwtService(testKeyPair.getPublic());
  }

  @Bean
  public ReactiveAuthenticationManager authenticationManager(JwtService jwtService) {
    return new JwtAuthenticationManager(jwtService);
  }

  @Bean
  public ServerAuthenticationConverter authenticationConverter(JwtService jwtService) {
    return new JwtServerAuthenticationConverter(jwtService);
  }

  @Bean
  public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
      ReactiveAuthenticationManager authenticationManager, ServerAuthenticationConverter authenticationConverter) {
    AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
    authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(customizer ->
            customizer
                .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .anyExchange().authenticated())
        .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .exceptionHandling(Customizer.withDefaults())
        .build();
  }
}
