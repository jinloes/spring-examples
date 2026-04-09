using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using salesforce_okta_csharp.Services;

namespace salesforce_okta_csharp.Controllers;

[ApiController]
[Route("salesforce")]
public class SalesforceController(SalesforceTokenService tokenService, SalesforceClient salesforceClient)
    : ControllerBase
{
    [HttpGet("query")]
    public async Task<IActionResult> Query(
        [FromHeader(Name = "Authorization")] string authHeader,
        [FromQuery] string soql
    )
    {
        var token = await tokenService.ExchangeAsync(ExtractBearerToken(authHeader));
        var result = await salesforceClient.QueryAsync(token, soql);
        return Ok(result);
    }

    [HttpGet("sobjects/{type}/{id}")]
    public async Task<IActionResult> GetRecord(
        [FromHeader(Name = "Authorization")] string authHeader,
        string type,
        string id
    )
    {
        var token = await tokenService.ExchangeAsync(ExtractBearerToken(authHeader));
        var result = await salesforceClient.GetRecordAsync(token, type, id);
        return Ok(result);
    }

    [HttpPost("sobjects/{type}")]
    public async Task<IActionResult> CreateRecord(
        [FromHeader(Name = "Authorization")] string authHeader,
        string type,
        [FromBody] JsonElement fields
    )
    {
        var token = await tokenService.ExchangeAsync(ExtractBearerToken(authHeader));
        var id = await salesforceClient.CreateRecordAsync(token, type, fields);
        return Ok(new { id });
    }

    [HttpPatch("sobjects/{type}/{id}")]
    public async Task<IActionResult> UpdateRecord(
        [FromHeader(Name = "Authorization")] string authHeader,
        string type,
        string id,
        [FromBody] JsonElement fields
    )
    {
        var token = await tokenService.ExchangeAsync(ExtractBearerToken(authHeader));
        await salesforceClient.UpdateRecordAsync(token, type, id, fields);
        return NoContent();
    }

    [HttpDelete("sobjects/{type}/{id}")]
    public async Task<IActionResult> DeleteRecord(
        [FromHeader(Name = "Authorization")] string authHeader,
        string type,
        string id
    )
    {
        var token = await tokenService.ExchangeAsync(ExtractBearerToken(authHeader));
        await salesforceClient.DeleteRecordAsync(token, type, id);
        return NoContent();
    }

    private static string ExtractBearerToken(string authHeader)
    {
        if (authHeader?.StartsWith("Bearer ") == true)
            return authHeader["Bearer ".Length..];
        throw new ArgumentException("Missing or invalid Authorization header");
    }
}
