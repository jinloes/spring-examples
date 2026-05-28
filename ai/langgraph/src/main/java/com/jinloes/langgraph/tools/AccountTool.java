package com.jinloes.langgraph.tools;

import com.jinloes.langgraph.observability.PendingActionsRegistry;
import com.jinloes.langgraph.observability.SessionContext;
import com.jinloes.langgraph.web.UiAction;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock account lookup for billing questions. Two tools, mirroring the refund flow:
 *
 * <ul>
 *   <li>{@link #lookupAccount(String)} — does the actual lookup once an email is known.
 *   <li>{@link #requestAccountEmail()} — queues a text-input field on the chat UI when no email has
 *       been provided yet, so the customer can type one without the bot having to manage prompting
 *       in free text.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountTool {

  private record Account(String plan, double balance, String renewalDate) {}

  private final Map<String, Account> accounts =
      Map.of(
          "alice@example.com", new Account("Pro", 0.0, "2026-12-01"),
          "bob@example.com", new Account("Free", 0.0, "n/a"),
          "carol@example.com", new Account("Team", 23.50, "2026-06-15"));

  private final SessionContext sessionContext;
  private final PendingActionsRegistry pendingActions;

  @Tool(
      "Look up a customer's billing details: plan name, outstanding balance, and renewal "
          + "date. Returns 'UNKNOWN_ACCOUNT' if the email is not on file.")
  public String lookupAccount(@P("Customer email address") String email) {
    log.info("AccountTool.lookupAccount email=%s".formatted(email));

    Account a = accounts.get(email.toLowerCase());
    if (a == null) {
      return "UNKNOWN_ACCOUNT";
    }
    return "plan=%s, balance=$%.2f, renewal=%s".formatted(a.plan(), a.balance(), a.renewalDate());
  }

  @Tool(
      "Prompt the customer to enter their email address. Call this when you need to look up "
          + "billing info but no email is present in the conversation yet. This queues a text "
          + "input field on the chat UI; your text response only needs to briefly mention that "
          + "you need the email — do not type out the email field yourself, the UI handles it.")
  public String requestAccountEmail() {
    log.info("AccountTool.requestAccountEmail");

    String sid = sessionContext.get();
    if (sid != null) {
      pendingActions.add(sid, UiAction.input("What's your email address?", "you@example.com"));
    }
    return "EMAIL_INPUT_QUEUED";
  }
}
