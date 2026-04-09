using System.Text.Json.Serialization;

namespace salesforce_okta_csharp.Models;

public record SalesforceTokenResponse(
    [property: JsonPropertyName("access_token")] string AccessToken,
    [property: JsonPropertyName("instance_url")] string InstanceUrl,
    [property: JsonPropertyName("token_type")] string TokenType
);
