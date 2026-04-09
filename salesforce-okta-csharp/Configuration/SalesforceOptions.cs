namespace salesforce_okta_csharp.Configuration;

public class SalesforceOptions
{
    public const string Section = "Salesforce";

    public string TokenUrl { get; set; } = "https://login.salesforce.com/services/oauth2/token";
    public string ClientId { get; set; } = string.Empty;
    public string Audience { get; set; } = "https://test.salesforce.com";
    public string UsernameSuffix { get; set; } = string.Empty;
    // RSA private key in PEM format (PKCS1 or PKCS8)
    public string PrivateKey { get; set; } = string.Empty;
}
