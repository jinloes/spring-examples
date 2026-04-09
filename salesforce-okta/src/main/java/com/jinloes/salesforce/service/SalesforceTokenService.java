package com.jinloes.salesforce.service;

import com.jinloes.salesforce.config.SalesforceProperties;
import com.jinloes.salesforce.model.SalesforceTokenResponse;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Implements the OAuth 2.0 JWT Bearer Flow (RFC 7523): exchanges an Okta token for a Salesforce
 * user-context access token by minting a signed JWT assertion.
 */
@Service
@RequiredArgsConstructor
public class SalesforceTokenService {

  private static final String JWT_BEARER_GRANT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
  private static final long JWT_EXPIRY_MS = 3 * 60 * 1000; // 3 minutes (Salesforce max)

  private final SalesforceProperties properties;
  private final RestClient restClient;

  public SalesforceTokenResponse exchange(String oktaToken) {
    String email = extractEmailFromJwt(oktaToken);
    String salesforceUsername = email + properties.usernameSuffix();
    String signedJwt = mintSalesforceJwt(salesforceUsername);

    var form = new LinkedMultiValueMap<String, String>();
    form.add("grant_type", JWT_BEARER_GRANT);
    form.add("assertion", signedJwt);

    return restClient
        .post()
        .uri(properties.tokenUrl())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(SalesforceTokenResponse.class);
  }

  private String extractEmailFromJwt(String jwt) {
    // Decode without verification — Salesforce enforces its own authz on the assertion.
    String[] parts = jwt.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
    String payload = new String(payloadBytes);
    String marker = "\"email\":\"";
    int start = payload.indexOf(marker) + marker.length();
    int end = payload.indexOf('"', start);
    return payload.substring(start, end);
  }

  private String mintSalesforceJwt(String username) {
    return Jwts.builder()
        .issuer(properties.clientId())
        .subject(username)
        .claim("aud", properties.audience())
        .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRY_MS))
        .signWith(loadPrivateKey())
        .compact();
  }

  private PrivateKey loadPrivateKey() {
    try {
      // Normalize: replace literal \n (common in env vars) and strip PEM headers/footers.
      String stripped =
          properties
              .privateKey()
              .replace("\\n", "\n")
              .replaceAll("-----[^-]+-----", "")
              .replaceAll("\\s+", "");
      byte[] der = Base64.getDecoder().decode(stripped);

      // Try PKCS8 first ("BEGIN PRIVATE KEY"). If it fails the key is PKCS1
      // ("BEGIN RSA PRIVATE KEY") — wrap it in a PrivateKeyInfo envelope so Java
      // can consume it via PKCS8EncodedKeySpec.
      try {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
      } catch (Exception pkcs8Failure) {
        PrivateKeyInfo pkInfo =
            new PrivateKeyInfo(
                new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption),
                RSAPrivateKey.getInstance(der));
        return new JcaPEMKeyConverter().getPrivateKey(pkInfo);
      }
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load Salesforce private key", e);
    }
  }

  private String padBase64(String value) {
    int padding = value.length() % 4;
    if (padding == 2) return value + "==";
    if (padding == 3) return value + "=";
    return value;
  }
}
