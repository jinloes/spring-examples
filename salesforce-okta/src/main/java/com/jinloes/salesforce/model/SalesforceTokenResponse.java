package com.jinloes.salesforce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesforceTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  /**
   * The Salesforce instance URL returned by the token endpoint. All subsequent API calls should use
   * this as the base URL.
   */
  @JsonProperty("instance_url")
  private String instanceUrl;

  @JsonProperty("token_type")
  private String tokenType;
}
