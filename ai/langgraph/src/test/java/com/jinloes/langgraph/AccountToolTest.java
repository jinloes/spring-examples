package com.jinloes.langgraph;

import static org.assertj.core.api.Assertions.assertThat;

import com.jinloes.langgraph.observability.PendingActionsRegistry;
import com.jinloes.langgraph.observability.SessionContext;
import com.jinloes.langgraph.tools.AccountTool;
import com.jinloes.langgraph.web.UiAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountToolTest {

  private final SessionContext session = new SessionContext();
  private final PendingActionsRegistry registry = new PendingActionsRegistry();
  private final AccountTool tool = new AccountTool(session, registry);

  @Test
  void lookupAccountReturnsPlanDetailsForKnownEmail() {
    assertThat(tool.lookupAccount("alice@example.com"))
        .contains("plan=Pro")
        .contains("balance=$0.00")
        .contains("renewal=2026-12-01");
  }

  @Test
  void lookupAccountIsCaseInsensitive() {
    assertThat(tool.lookupAccount("ALICE@EXAMPLE.COM")).contains("plan=Pro");
  }

  @Test
  void lookupAccountReturnsUnknownForMissingEmail() {
    assertThat(tool.lookupAccount("nobody@example.com")).isEqualTo("UNKNOWN_ACCOUNT");
  }

  @Test
  void requestAccountEmailQueuesAnInputActionForTheCurrentSession() {
    session.set("sess-1");
    try {
      String result = tool.requestAccountEmail();
      // The LLM-facing return is a flag, not text the customer will see — the UI input field
      // is what surfaces to the user.
      assertThat(result).isEqualTo("EMAIL_INPUT_QUEUED");
    } finally {
      session.clear();
    }

    List<UiAction> queued = registry.drain("sess-1");
    assertThat(queued).hasSize(1);
    UiAction a = queued.get(0);
    assertThat(a.type()).isEqualTo("input");
    assertThat(a.placeholder()).isEqualTo("you@example.com");
    assertThat(a.prompt()).contains("email");
  }

  @Test
  void requestAccountEmailSkipsActionWhenNoSessionInContext() {
    // Defensive: tool may be invoked outside a /triage call. Should not NPE.
    assertThat(tool.requestAccountEmail()).isEqualTo("EMAIL_INPUT_QUEUED");
    assertThat(registry.drain("any")).isEmpty();
  }
}
