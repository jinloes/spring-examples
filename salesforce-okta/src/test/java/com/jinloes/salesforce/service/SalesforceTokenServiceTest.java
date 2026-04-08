package com.jinloes.salesforce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jinloes.salesforce.config.SalesforceProperties;
import com.jinloes.salesforce.model.SalesforceTokenResponse;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesforceTokenServiceTest {

  @Mock RestClient restClient;

  private SalesforceTokenService service;
  private KeyPair keyPair;

  // Captured mocks for assertion in nested tests
  private RestClient.RequestBodySpec mockBodySpec;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    keyPair = gen.generateKeyPair();

    service =
        new SalesforceTokenService(
            new SalesforceProperties(
                "https://test.salesforce.com/services/oauth2/token",
                "test-client-id",
                "https://test.salesforce.com",
                ".qa",
                toPkcs8Pem(keyPair.getPrivate())),
            restClient);
  }

  @Nested
  class Exchange {

    @Test
    void extractsEmailFromOktaTokenAndAppendsSuffix() {
      stubRestClient(sfTokenResponse());

      service.exchange(buildOktaJwt("user@linkedin.com"));

      assertThat(capturedAssertionClaims().getSubject()).isEqualTo("user@linkedin.com.qa");
    }

    @Test
    void mintsJwtWithCorrectIssuerAndAudience() {
      stubRestClient(sfTokenResponse());

      service.exchange(buildOktaJwt("user@linkedin.com"));

      var claims = capturedAssertionClaims();
      assertThat(claims.getIssuer()).isEqualTo("test-client-id");
      assertThat(claims.getAudience()).containsExactly("https://test.salesforce.com");
    }

    @Test
    void mintsJwtWithExpiry() {
      stubRestClient(sfTokenResponse());

      service.exchange(buildOktaJwt("user@linkedin.com"));

      assertThat(capturedAssertionClaims().getExpiration()).isNotNull();
    }

    @Test
    void postsJwtBearerGrantType() {
      stubRestClient(sfTokenResponse());

      service.exchange(buildOktaJwt("user@linkedin.com"));

      assertThat(capturedForm().getFirst("grant_type"))
          .isEqualTo("urn:ietf:params:oauth:grant-type:jwt-bearer");
    }

    @Test
    void returnsTokenResponse() {
      var expected = sfTokenResponse();
      stubRestClient(expected);

      assertThat(service.exchange(buildOktaJwt("user@linkedin.com"))).isEqualTo(expected);
    }
  }

  // --- helpers ---

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void stubRestClient(SalesforceTokenResponse response) {
    var mockUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    mockBodySpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(mockUriSpec);
    when(mockUriSpec.uri(anyString())).thenReturn(mockBodySpec);
    when(mockBodySpec.contentType(any())).thenReturn(mockBodySpec);
    when(mockBodySpec.body((Object) any())).thenReturn(mockBodySpec);
    when(mockBodySpec.retrieve()).thenReturn(mockResponseSpec);
    when(mockResponseSpec.body(SalesforceTokenResponse.class)).thenReturn(response);
  }

  @SuppressWarnings("unchecked")
  private MultiValueMap<String, String> capturedForm() {
    var captor = ArgumentCaptor.forClass(MultiValueMap.class);
    verify(mockBodySpec).body(captor.capture());
    return captor.getValue();
  }

  private io.jsonwebtoken.Claims capturedAssertionClaims() {
    String assertion = capturedForm().getFirst("assertion");
    return Jwts.parser()
        .verifyWith(keyPair.getPublic())
        .build()
        .parseSignedClaims(assertion)
        .getPayload();
  }

  private SalesforceTokenResponse sfTokenResponse() {
    var token = new SalesforceTokenResponse();
    token.setAccessToken("sf-access-token");
    token.setInstanceUrl("https://linkedin--qa.sandbox.my.salesforce.com");
    return token;
  }

  private String buildOktaJwt(String email) {
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(("{\"sub\":\"user123\",\"email\":\"" + email + "\"}").getBytes());
    return header + "." + payload + ".fakesignature";
  }

  private String toPkcs8Pem(PrivateKey key) {
    String b64 = Base64.getEncoder().encodeToString(key.getEncoded());
    return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----";
  }
}
