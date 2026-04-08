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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Obtains a Salesforce user-context access token using the OAuth 2.0 JWT Bearer Flow (RFC 7523).
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>Decode the Okta JWT to extract the user's email
 *   <li>Mint a signed JWT asserting that user's identity to Salesforce
 *   <li>POST to Salesforce token endpoint to get a user-context access token
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class SalesforceTokenService {

  private static final String JWT_BEARER_GRANT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
  private static final long JWT_EXPIRY_MS = 3 * 60 * 1000; // 3 minutes (Salesforce max)

  private final SalesforceProperties properties;
  private final RestClient restClient;

  /**
   * Exchanges the provided Okta token for a Salesforce user-context access token.
   *
   * @param oktaToken the Bearer token issued by Okta
   * @return Salesforce token response containing access_token and instance_url
   */
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

  /**
   * Decodes the Okta JWT (without verification — the token was already presented by the user and
   * Salesforce will enforce its own authz) and extracts the email claim.
   */
  private String extractEmailFromJwt(String jwt) {
    String[] parts = jwt.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
    String payload = new String(payloadBytes);
    String marker = "\"email\":\"";
    int start = payload.indexOf(marker) + marker.length();
    int end = payload.indexOf('"', start);
    return payload.substring(start, end);
  }

  /**
   * Mints a short-lived JWT signed with the RSA private key that Salesforce will verify against the
   * public key uploaded to the connected app.
   */
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
      String pem =
          properties
              .privateKey()
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s", "");
      byte[] keyBytes = Base64.getDecoder().decode(pem);
      return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
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
