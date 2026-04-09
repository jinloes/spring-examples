using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.OpenSsl;
using Org.BouncyCastle.Security;
using salesforce_okta_csharp.Configuration;
using salesforce_okta_csharp.Models;

namespace salesforce_okta_csharp.Services;

/// <summary>
/// Implements the OAuth 2.0 JWT Bearer Flow (RFC 7523): exchanges an Okta token for a
/// Salesforce user-context access token by minting a signed JWT assertion.
/// </summary>
public class SalesforceTokenService(IOptions<SalesforceOptions> options, HttpClient httpClient)
{
    private const string JwtBearerGrant = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static readonly TimeSpan JwtExpiry = TimeSpan.FromMinutes(3); // Salesforce max

    private readonly SalesforceOptions _options = options.Value;

    public async Task<SalesforceTokenResponse> ExchangeAsync(string oktaToken)
    {
        var email = ExtractEmailFromJwt(oktaToken);
        var salesforceUsername = email + _options.UsernameSuffix;
        var signedJwt = MintSalesforceJwt(salesforceUsername);

        var form = new Dictionary<string, string>
        {
            ["grant_type"] = JwtBearerGrant,
            ["assertion"] = signedJwt,
        };

        var response = await httpClient.PostAsync(
            _options.TokenUrl,
            new FormUrlEncodedContent(form)
        );
        response.EnsureSuccessStatusCode();

        var body = await response.Content.ReadAsStringAsync();
        return JsonSerializer.Deserialize<SalesforceTokenResponse>(body)
            ?? throw new InvalidOperationException("Empty token response from Salesforce");
    }

    private string ExtractEmailFromJwt(string jwt)
    {
        // Decode without verification — Salesforce enforces its own authz on the assertion.
        var parts = jwt.Split('.');
        var payload = Encoding.UTF8.GetString(Base64UrlDecode(parts[1]));
        using var doc = JsonDocument.Parse(payload);
        return doc.RootElement.GetProperty("email").GetString()
            ?? throw new InvalidOperationException("No email claim in Okta token");
    }

    private string MintSalesforceJwt(string username)
    {
        var rsa = LoadPrivateKey();
        var credentials = new SigningCredentials(new RsaSecurityKey(rsa), SecurityAlgorithms.RsaSha256);

        var token = new JwtSecurityToken(
            issuer: _options.ClientId,
            audience: _options.Audience,
            claims: [new Claim(JwtRegisteredClaimNames.Sub, username)],
            expires: DateTime.UtcNow.Add(JwtExpiry),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    private RSA LoadPrivateKey()
    {
        // Normalize: replace literal \n (common in env vars) and let BouncyCastle
        // handle both PKCS1 ("BEGIN RSA PRIVATE KEY") and PKCS8 ("BEGIN PRIVATE KEY").
        var pem = _options.PrivateKey.Replace("\\n", "\n");

        using var reader = new StringReader(pem);
        var pemReader = new PemReader(reader);
        var obj = pemReader.ReadObject();

        AsymmetricKeyParameter privateKeyParam = obj switch
        {
            AsymmetricCipherKeyPair keyPair => keyPair.Private,
            AsymmetricKeyParameter key => key,
            _ => throw new InvalidOperationException($"Unexpected PEM object type: {obj?.GetType().Name}"),
        };

        var rsaParams = DotNetUtilities.ToRSAParameters((RsaPrivateCrtKeyParameters)privateKeyParam);
        var rsa = RSA.Create();
        rsa.ImportParameters(rsaParams);
        return rsa;
    }

    private static byte[] Base64UrlDecode(string input)
    {
        var padded = (input.Length % 4) switch
        {
            2 => input + "==",
            3 => input + "=",
            _ => input,
        };
        return Convert.FromBase64String(padded.Replace("-", "+").Replace("_", "/"));
    }
}
