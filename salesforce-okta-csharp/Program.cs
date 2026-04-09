using salesforce_okta_csharp.Configuration;
using salesforce_okta_csharp.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.Configure<SalesforceOptions>(
    builder.Configuration.GetSection(SalesforceOptions.Section)
);
builder.Services.AddHttpClient<SalesforceTokenService>();
builder.Services.AddHttpClient<SalesforceClient>();

var app = builder.Build();

app.UseAuthorization();
app.MapControllers();
app.Run();
