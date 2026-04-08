package com.jinloes.salesforce.client;

import com.jinloes.salesforce.model.SalesforceQueryResult;
import com.jinloes.salesforce.model.SalesforceTokenResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class SalesforceClient {

  private static final String QUERY_PATH = "/services/data/v59.0/query";
  private static final String SOBJECT_PATH = "/services/data/v59.0/sobjects/{type}/{id}";

  private final RestClient restClient;

  public SalesforceQueryResult query(SalesforceTokenResponse token, String soql) {
    return clientFor(token)
        .get()
        .uri(uriBuilder -> uriBuilder.path(QUERY_PATH).queryParam("q", soql).build())
        .retrieve()
        .body(SalesforceQueryResult.class);
  }

  public Map<?, ?> getRecord(SalesforceTokenResponse token, String type, String id) {
    return clientFor(token).get().uri(SOBJECT_PATH, type, id).retrieve().body(Map.class);
  }

  public String createRecord(
      SalesforceTokenResponse token, String type, Map<String, Object> fields) {
    var response =
        clientFor(token)
            .post()
            .uri("/services/data/v59.0/sobjects/{type}", type)
            .body(fields)
            .retrieve()
            .body(Map.class);
    return response != null ? (String) response.get("id") : null;
  }

  public void updateRecord(
      SalesforceTokenResponse token, String type, String id, Map<String, Object> fields) {
    clientFor(token).patch().uri(SOBJECT_PATH, type, id).body(fields).retrieve().toBodilessEntity();
  }

  public void deleteRecord(SalesforceTokenResponse token, String type, String id) {
    clientFor(token).delete().uri(SOBJECT_PATH, type, id).retrieve().toBodilessEntity();
  }

  /**
   * Builds a per-request RestClient rooted at the instance_url returned by the token exchange, with
   * the Salesforce Bearer token attached.
   */
  private RestClient clientFor(SalesforceTokenResponse token) {
    return restClient
        .mutate()
        .baseUrl(token.getInstanceUrl())
        .defaultHeader("Authorization", "Bearer " + token.getAccessToken())
        .build();
  }
}
