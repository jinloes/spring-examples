package com.jinloes.salesforce.client;

import com.jinloes.salesforce.model.SalesforceQueryResult;
import com.jinloes.salesforce.model.SalesforceTokenResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesforceClient {

  private static final String QUERY_PATH = "/services/data/v59.0/query";
  private static final String SOBJECT_PATH = "/services/data/v59.0/sobjects/{type}/{id}";

  private final RestClient restClient;

  public SalesforceQueryResult query(SalesforceTokenResponse token, String soql) {
    log.debug("Executing SOQL query: {}", soql);
    try {
      SalesforceQueryResult result =
          clientFor(token)
              .get()
              .uri(uriBuilder -> uriBuilder.path(QUERY_PATH).queryParam("q", soql).build())
              .retrieve()
              .body(SalesforceQueryResult.class);
      log.debug("Query returned {} record(s)", result != null ? result.getTotalSize() : 0);
      return result;
    } catch (HttpStatusCodeException e) {
      log.error(
          "Salesforce query failed with status {}: {}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw e;
    }
  }

  public Map<?, ?> getRecord(SalesforceTokenResponse token, String type, String id) {
    log.debug("Fetching {} record id={}", type, id);
    try {
      return clientFor(token).get().uri(SOBJECT_PATH, type, id).retrieve().body(Map.class);
    } catch (HttpStatusCodeException e) {
      log.error("Failed to get {} id={}: status {}", type, id, e.getStatusCode());
      throw e;
    }
  }

  public String createRecord(
      SalesforceTokenResponse token, String type, Map<String, Object> fields) {
    log.debug("Creating {} record with fields: {}", type, fields.keySet());
    try {
      var response =
          clientFor(token)
              .post()
              .uri("/services/data/v59.0/sobjects/{type}", type)
              .body(fields)
              .retrieve()
              .body(Map.class);
      String id = response != null ? (String) response.get("id") : null;
      log.info("Created {} record id={}", type, id);
      return id;
    } catch (HttpStatusCodeException e) {
      log.error("Failed to create {} record: status {}", type, e.getStatusCode());
      throw e;
    }
  }

  public void updateRecord(
      SalesforceTokenResponse token, String type, String id, Map<String, Object> fields) {
    log.debug("Updating {} record id={} fields={}", type, id, fields.keySet());
    try {
      clientFor(token)
          .patch()
          .uri(SOBJECT_PATH, type, id)
          .body(fields)
          .retrieve()
          .toBodilessEntity();
      log.debug("Updated {} record id={}", type, id);
    } catch (HttpStatusCodeException e) {
      log.error("Failed to update {} id={}: status {}", type, id, e.getStatusCode());
      throw e;
    }
  }

  public void deleteRecord(SalesforceTokenResponse token, String type, String id) {
    log.debug("Deleting {} record id={}", type, id);
    try {
      clientFor(token).delete().uri(SOBJECT_PATH, type, id).retrieve().toBodilessEntity();
      log.info("Deleted {} record id={}", type, id);
    } catch (HttpStatusCodeException e) {
      log.error("Failed to delete {} id={}: status {}", type, id, e.getStatusCode());
      throw e;
    }
  }

  /**
   * Builds a per-request RestClient rooted at the instance_url returned by the token exchange, with
   * the Salesforce Bearer token attached.
   */
  private RestClient clientFor(SalesforceTokenResponse token) {
    return restClient
        .mutate()
        .baseUrl(token.instanceUrl())
        .defaultHeader("Authorization", "Bearer " + token.accessToken())
        .build();
  }
}
