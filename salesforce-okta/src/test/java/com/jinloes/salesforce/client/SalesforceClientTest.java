package com.jinloes.salesforce.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jinloes.salesforce.model.SalesforceQueryResult;
import com.jinloes.salesforce.model.SalesforceTokenResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesforceClientTest {

  @Mock RestClient restClient;

  private SalesforceClient client;
  private RestClient.Builder builder;
  private RestClient builtClient;

  @BeforeEach
  void setUp() {
    client = new SalesforceClient(restClient);

    builder = mock(RestClient.Builder.class);
    builtClient = mock(RestClient.class);

    when(restClient.mutate()).thenReturn(builder);
    when(builder.baseUrl(anyString())).thenReturn(builder);
    when(builder.defaultHeader(anyString(), anyString())).thenReturn(builder);
    when(builder.build()).thenReturn(builtClient);
  }

  @Nested
  class Query {

    @Test
    void returnsQueryResult() {
      var expected = new SalesforceQueryResult();
      expected.setTotalSize(1);
      expected.setRecords(List.of(Map.of("Id", "001xxx")));

      stubGetChain(SalesforceQueryResult.class, expected);

      assertThat(client.query(sfToken(), "SELECT Id FROM Case")).isEqualTo(expected);
    }

    @Test
    void configuresClientWithTokenAndInstanceUrl() {
      stubGetChain(SalesforceQueryResult.class, new SalesforceQueryResult());

      client.query(sfToken(), "SELECT Id FROM Case");

      verify(builder).baseUrl("https://linkedin--qa.sandbox.my.salesforce.com");
      verify(builder).defaultHeader("Authorization", "Bearer sf-access-token");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void propagatesHttpErrorOnQueryFailure() {
      RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
      RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
      RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

      when(builtClient.get()).thenReturn(getSpec);
      doReturn(headersSpec).when(getSpec).uri(isA(java.util.function.Function.class));
      when(headersSpec.retrieve()).thenReturn(responseSpec);
      when(responseSpec.body(SalesforceQueryResult.class))
          .thenThrow(
              HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null));

      assertThatThrownBy(() -> client.query(sfToken(), "SELECT Id FROM Case"))
          .isInstanceOf(HttpClientErrorException.class)
          .extracting(e -> ((HttpClientErrorException) e).getStatusCode())
          .isEqualTo(HttpStatus.FORBIDDEN);
    }
  }

  @Nested
  class GetRecord {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void returnsRecord() {
      Map<?, ?> expected = Map.of("Id", "001xxx", "Subject", "Test");
      RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
      RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
      RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

      when(builtClient.get()).thenReturn(getSpec);
      doReturn(headersSpec).when(getSpec).uri(anyString(), eq("Case"), eq("001xxx"));
      when(headersSpec.retrieve()).thenReturn(responseSpec);
      doReturn(expected).when(responseSpec).body(Map.class);

      assertThat(client.getRecord(sfToken(), "Case", "001xxx")).isEqualTo(expected);
    }
  }

  @Nested
  class CreateRecord {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void returnsNewRecordId() {
      RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
      RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
      RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

      when(builtClient.post()).thenReturn(postSpec);
      doReturn(bodySpec).when(postSpec).uri(anyString(), eq("Case"));
      when(bodySpec.body((Object) any())).thenReturn(bodySpec);
      when(bodySpec.retrieve()).thenReturn(responseSpec);
      when(responseSpec.body(Map.class)).thenReturn(Map.of("id", "001newid"));

      assertThat(client.createRecord(sfToken(), "Case", Map.of("Subject", "Test")))
          .isEqualTo("001newid");
    }
  }

  // --- helpers ---

  @SuppressWarnings({"unchecked", "rawtypes"})
  private <T> void stubGetChain(Class<T> responseType, T response) {
    RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

    when(builtClient.get()).thenReturn(getSpec);
    doReturn(headersSpec).when(getSpec).uri(isA(java.util.function.Function.class));
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(responseType)).thenReturn(response);
  }

  private SalesforceTokenResponse sfToken() {
    return new SalesforceTokenResponse(
        "sf-access-token", "https://linkedin--qa.sandbox.my.salesforce.com", "Bearer");
  }
}
