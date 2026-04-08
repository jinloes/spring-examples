package com.jinloes.salesforce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Salesforce connection properties bound from the {@code salesforce.*} namespace.
 *
 * @param tokenUrl Salesforce OAuth token endpoint
 * @param clientId Salesforce connected app client ID (Consumer Key)
 * @param audience JWT audience — https://test.salesforce.com for sandboxes,
 *     https://login.salesforce.com for production
 * @param usernameSuffix suffix appended to the Okta email to form the Salesforce username (e.g.
 *     .qa, .uat)
 * @param privateKey RSA private key in PKCS8 PEM format including headers
 */
@ConfigurationProperties(prefix = "salesforce")
public record SalesforceProperties(
    String tokenUrl,
    String clientId,
    String audience,
    @DefaultValue("") String usernameSuffix,
    String privateKey) {}
