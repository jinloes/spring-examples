package com.jinloes.langgraph.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock docs / KB search. In a real system this would hit a vector store; here we return canned
 * snippets keyed off keywords so the example is deterministic.
 */
@Slf4j
@Component
public class DocsSearchTool {

  private static final Map<String, String> DOCS =
      Map.of(
          "login",
          "Reset your password at /account/reset. Tokens expire after 24h.",
          "password",
          "Reset your password at /account/reset. Tokens expire after 24h.",
          "2fa",
          "Enroll a TOTP authenticator at /account/security. Backup codes are" + " shown once.",
          "api",
          "API tokens live in /developer/tokens. Use Bearer auth on the" + " Authorization header.",
          "rate limit",
          "Default rate limit is 100 req/min per token. Contact support to raise" + " it.",
          "sync",
          "Data sync runs every 15 minutes. Manual trigger: POST /sync/run.");

  @Tool(
      "Search the technical knowledge base for an answer. Returns up to 3 short snippets "
          + "relevant to the query, or NO_MATCH if nothing is found.")
  public String searchDocs(@P("Free-text technical question or keywords") String query) {
    log.info("DocsSearchTool.searchDocs query=%s".formatted(query));

    String lower = query.toLowerCase();
    List<String> hits =
        DOCS.entrySet().stream()
            .filter(e -> lower.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .distinct()
            .limit(3)
            .toList();

    return hits.isEmpty() ? "NO_MATCH" : String.join("\n---\n", hits);
  }
}
