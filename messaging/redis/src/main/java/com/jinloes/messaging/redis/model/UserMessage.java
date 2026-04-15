package com.jinloes.messaging.redis.model;

/**
 * Envelope published to the Redis {@code user-messages} channel.
 *
 * <p>Every app instance receives every envelope. Each instance checks whether {@code to} is
 * connected locally and delivers only if so — see {@link
 * com.jinloes.messaging.redis.messaging.UserMessageForwarder}.
 */
public record UserMessage(String to, String body) {}
