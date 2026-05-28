package com.jinloes.langgraph.web;

import java.util.List;

/**
 * Structured directive the backend sends to the chat UI alongside the natural-language response.
 * Lets a tool render interactive UI elements without forcing the LLM to emit parseable markers in
 * its text channel.
 *
 * <p>Two flavours:
 *
 * <ul>
 *   <li>{@code select} — click one of {@code options} to send {@code option.value} as the next user
 *       message. Use the {@link #select(String, List)} factory.
 *   <li>{@code input} — a labelled text field with a placeholder; submitting it sends the typed
 *       value as the next user message. Use the {@link #input(String, String)} factory.
 * </ul>
 *
 * @param type one of {@code "select"} or {@code "input"}
 * @param prompt short label rendered above the action
 * @param options selectable items (only meaningful for {@code select}; null for {@code input})
 * @param placeholder grey hint text inside the input (only meaningful for {@code input}; null for
 *     {@code select})
 */
public record UiAction(String type, String prompt, List<Option> options, String placeholder) {

  public record Option(String value, String label) {}

  public static UiAction select(String prompt, List<Option> options) {
    return new UiAction("select", prompt, options, null);
  }

  public static UiAction input(String prompt, String placeholder) {
    return new UiAction("input", prompt, null, placeholder);
  }
}
