package com.jinloes.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.security.PublicKey;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtService {
  private final JwtParser jwtParser;

  public JwtService(PublicKey jwtPublicKey) {
    this.jwtParser = Jwts.parser()
        .verifyWith(jwtPublicKey)
        .build();
  }

  public boolean isTokenValid(String token) {
    Claims claims;
    try {
      claims = getClaims(token);
    } catch (ExpiredJwtException e) {
      log.debug("Expired token", e);
      return false;
    } catch (MalformedJwtException e) {
      log.debug("Invalid token", e);
      return false;
    } catch (SignatureException e) {
      log.debug("Invalid signature", e);
      return false;
    }
    return Optional.ofNullable(claims.getExpiration())
        .map(date -> date.after(new Date()))
        .orElse(true);
  }

  public Claims getClaims(String token) {
    return jwtParser.parseSignedClaims(token)
        .getPayload();
  }
}
