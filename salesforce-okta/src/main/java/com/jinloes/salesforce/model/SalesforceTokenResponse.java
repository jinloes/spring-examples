package com.jinloes.salesforce.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the OAuth 2.0 token response returned by the Salesforce token endpoint.
 *
 * @param accessToken the Bearer token to use in subsequent Salesforce API calls
 * @param instanceUrl the Salesforce instance base URL; all API requests must be rooted here
 * @param tokenType always "Bearer" for the JWT Bearer flow
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesforceTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("instance_url") String instanceUrl,
    @JsonProperty("token_type") String tokenType) {}
