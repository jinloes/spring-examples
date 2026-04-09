using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using salesforce_okta_csharp.Models;

namespace salesforce_okta_csharp.Services;

public class SalesforceClient(HttpClient httpClient)
{
    private const string ApiVersion = "v59.0";

    public async Task<JsonDocument> QueryAsync(SalesforceTokenResponse token, string soql)
    {
        using var request = new HttpRequestMessage(
            HttpMethod.Get,
            $"{token.InstanceUrl}/services/data/{ApiVersion}/query?q={Uri.EscapeDataString(soql)}"
        );
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token.AccessToken);

        var response = await httpClient.SendAsync(request);
        response.EnsureSuccessStatusCode();
        return JsonDocument.Parse(await response.Content.ReadAsStringAsync());
    }

    public async Task<JsonDocument> GetRecordAsync(SalesforceTokenResponse token, string type, string id)
    {
        using var request = new HttpRequestMessage(
            HttpMethod.Get,
            $"{token.InstanceUrl}/services/data/{ApiVersion}/sobjects/{type}/{id}"
        );
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token.AccessToken);

        var response = await httpClient.SendAsync(request);
        response.EnsureSuccessStatusCode();
        return JsonDocument.Parse(await response.Content.ReadAsStringAsync());
    }

    public async Task<string> CreateRecordAsync(
        SalesforceTokenResponse token,
        string type,
        object fields
    )
    {
        using var request = new HttpRequestMessage(
            HttpMethod.Post,
            $"{token.InstanceUrl}/services/data/{ApiVersion}/sobjects/{type}"
        );
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token.AccessToken);
        request.Content = new StringContent(
            JsonSerializer.Serialize(fields),
            Encoding.UTF8,
            "application/json"
        );

        var response = await httpClient.SendAsync(request);
        response.EnsureSuccessStatusCode();
        using var doc = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        return doc.RootElement.GetProperty("id").GetString()!;
    }

    public async Task UpdateRecordAsync(
        SalesforceTokenResponse token,
        string type,
        string id,
        object fields
    )
    {
        using var request = new HttpRequestMessage(
            HttpMethod.Patch,
            $"{token.InstanceUrl}/services/data/{ApiVersion}/sobjects/{type}/{id}"
        );
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token.AccessToken);
        request.Content = new StringContent(
            JsonSerializer.Serialize(fields),
            Encoding.UTF8,
            "application/json"
        );

        var response = await httpClient.SendAsync(request);
        response.EnsureSuccessStatusCode();
    }

    public async Task DeleteRecordAsync(SalesforceTokenResponse token, string type, string id)
    {
        using var request = new HttpRequestMessage(
            HttpMethod.Delete,
            $"{token.InstanceUrl}/services/data/{ApiVersion}/sobjects/{type}/{id}"
        );
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token.AccessToken);

        var response = await httpClient.SendAsync(request);
        response.EnsureSuccessStatusCode();
    }
}
