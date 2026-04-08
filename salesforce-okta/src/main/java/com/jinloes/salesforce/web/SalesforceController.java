package com.jinloes.salesforce.web;

import com.jinloes.salesforce.client.SalesforceClient;
import com.jinloes.salesforce.model.SalesforceQueryResult;
import com.jinloes.salesforce.service.SalesforceTokenService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/salesforce")
@RequiredArgsConstructor
public class SalesforceController {

  private final SalesforceTokenService tokenService;
  private final SalesforceClient salesforceClient;

  @GetMapping("/query")
  public SalesforceQueryResult query(
      @RequestHeader("Authorization") String authHeader, @RequestParam String soql) {
    var token = tokenService.exchange(extractBearerToken(authHeader));
    return salesforceClient.query(token, soql);
  }

  @GetMapping("/sobjects/{type}/{id}")
  public Map<?, ?> getRecord(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String type,
      @PathVariable String id) {
    var token = tokenService.exchange(extractBearerToken(authHeader));
    return salesforceClient.getRecord(token, type, id);
  }

  @PostMapping("/sobjects/{type}")
  public ResponseEntity<Map<String, String>> createRecord(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String type,
      @RequestBody Map<String, Object> fields) {
    var token = tokenService.exchange(extractBearerToken(authHeader));
    String id = salesforceClient.createRecord(token, type, fields);
    return ResponseEntity.ok(Map.of("id", id));
  }

  @PatchMapping("/sobjects/{type}/{id}")
  public ResponseEntity<Void> updateRecord(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String type,
      @PathVariable String id,
      @RequestBody Map<String, Object> fields) {
    var token = tokenService.exchange(extractBearerToken(authHeader));
    salesforceClient.updateRecord(token, type, id, fields);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/sobjects/{type}/{id}")
  public ResponseEntity<Void> deleteRecord(
      @RequestHeader("Authorization") String authHeader,
      @PathVariable String type,
      @PathVariable String id) {
    var token = tokenService.exchange(extractBearerToken(authHeader));
    salesforceClient.deleteRecord(token, type, id);
    return ResponseEntity.noContent().build();
  }

  private String extractBearerToken(String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    throw new IllegalArgumentException("Missing or invalid Authorization header");
  }
}
