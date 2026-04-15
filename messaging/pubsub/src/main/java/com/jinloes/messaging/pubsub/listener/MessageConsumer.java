package com.jinloes.messaging.pubsub.listener;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;

@Configuration
@Slf4j
public class MessageConsumer {

  @Bean
  public MessageChannel inputMessageChannel() {
    return new PublishSubscribeChannel();
  }

  @Bean
  public PubSubInboundChannelAdapter inboundChannelAdapter(
      MessageChannel inputMessageChannel,
      PubSubTemplate pubSubTemplate,
      @Value("${app.pubsub.subscription}") String subscription) {
    PubSubInboundChannelAdapter adapter =
        new PubSubInboundChannelAdapter(pubSubTemplate, subscription);
    adapter.setOutputChannel(inputMessageChannel);
    adapter.setAckMode(AckMode.MANUAL);
    adapter.setPayloadType(String.class);
    return adapter;
  }

  @ServiceActivator(inputChannel = "inputMessageChannel")
  public void onMessage(
      String payload,
      @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
    log.info("Received from subscription: {}", payload);
    message.ack();
  }
}
