package org.datarocks.lwgs.searchindex.client.configuration;

import lombok.Getter;
import org.datarocks.lwgs.searchindex.client.service.amqp.Exchanges;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Getter
@Configuration
@EnableScheduling
public class ApplicationConfiguration {
  @Bean
  public TopicExchange topicExchange() {
    return new TopicExchange(Exchanges.LWGS);
  }

  @Bean
  public TopicExchange stateTopicExchange() {
    return new TopicExchange(Exchanges.LWGS_STATE);
  }

  @Bean
  public Queue seedPartialIncomingQueue() {
    return new Queue(Queues.PERSONDATA_PARTIAL_INCOMING, true);
  }

  @Bean
  public Binding seedPartialIncomingBinding(
      TopicExchange topicExchange, Queue seedPartialIncomingQueue) {
    return BindingBuilder.bind(seedPartialIncomingQueue)
        .to(topicExchange)
        .with(Topics.PERSONDATA_PARTIAL_INCOMING);
  }

  @Bean
  public Queue seedPartialOutgoingQueue() {
    return new Queue(Queues.PERSONDATA_PARTIAL_OUTGOING, true);
  }

  @Bean
  public Binding seedPartialOutgoingBinding(
      TopicExchange topicExchange, Queue seedPartialOutgoingQueue) {
    return BindingBuilder.bind(seedPartialOutgoingQueue)
        .to(topicExchange)
        .with(Topics.PERSONDATA_PARTIAL_OUTGOING);
  }

  @Bean
  public Queue seedPartialFailedQueue() {
    return new Queue(Queues.PERSONDATA_PARTIAL_FAILED, true);
  }

  @Bean
  public Binding seedPartialFailedBinding(
      TopicExchange topicExchange, Queue seedPartialFailedQueue) {
    return BindingBuilder.bind(seedPartialFailedQueue)
        .to(topicExchange)
        .with(Topics.PERSONDATA_PARTIAL_FAILED);
  }

  @Bean
  public Queue seedFullIncomingQueue() {
    return new Queue(Queues.PERSONDATA_FULL_INCOMING, true);
  }

  @Bean
  public Binding seedFullIncomingBinding(TopicExchange topicExchange, Queue seedFullIncomingQueue) {
    return BindingBuilder.bind(seedFullIncomingQueue)
        .to(topicExchange)
        .with(Topics.PERSONDATA_FULL_INCOMING);
  }

  @Bean
  public Queue seedFullOutgoingQueue() {
    return new Queue(Queues.PERSONDATA_FULL_OUTGOING, true);
  }

  @Bean
  public Binding seedFullOutgoingBinding(TopicExchange topicExchange, Queue seedFullOutgoingQueue) {
    return BindingBuilder.bind(seedFullOutgoingQueue)
        .to(topicExchange)
        .with(Topics.PERSONDATA_FULL_OUTGOING);
  }

  @Bean
  public Queue seedFullFailedQueue() {
    return new Queue(Queues.PERSONDATA_FULL_FAILED, true);
  }

  @Bean
  public Binding seedFullFailedBinding(TopicExchange topicExchange, Queue seedFullFailedQueue) {
    return BindingBuilder.bind(seedFullFailedQueue)
        .to(topicExchange)
        .with(Topics.PERSONDATA_FULL_FAILED);
  }

  @Bean
  public Queue partialSeedQueue() {
    return new Queue(Queues.SEDEX_OUTBOX, true);
  }

  @Bean
  public Binding partialSeedBinding(TopicExchange topicExchange, Queue partialSeedQueue) {
    return BindingBuilder.bind(partialSeedQueue).to(topicExchange).with(Topics.SEDEX_OUTBOX);
  }

  @Bean
  public Queue sedexReceiptsQueue() {
    return new Queue(Queues.SEDEX_RECEIPTS, true);
  }

  @Bean
  public Binding sedexReceiptsBinding(TopicExchange topicExchange, Queue sedexReceiptsQueue) {
    return BindingBuilder.bind(sedexReceiptsQueue).to(topicExchange).with(Topics.SEDEX_RECEIPTS);
  }

  @Bean
  public Queue sedexStateQueue() {
    return new Queue(Queues.SEDEX_STATE, true);
  }

  @Bean
  public Binding sedexStateBinding(TopicExchange topicExchange, Queue sedexStateQueue) {
    return BindingBuilder.bind(sedexStateQueue).to(topicExchange).with(Topics.SEDEX_STATUS_UPDATED);
  }

  @Bean
  public Queue jobStateQueue() {
    return new Queue(Queues.JOB_STATE, true);
  }

  @Bean
  public Queue transactionStateQueue() {
    return new Queue(Queues.TRANSACTION_STATE, true);
  }

  @Bean
  public Queue businessLogQueue() {
    return new Queue(Queues.BUSINESS_LOG, true);
  }

  @Bean
  public Binding jobStateBinding(TopicExchange topicExchange, Queue jobStateQueue) {
    return BindingBuilder.bind(jobStateQueue).to(topicExchange).with(Topics.PERSONDATA_CATCH_ALL);
  }

  @Bean
  public Binding transactionStateBinding(
      TopicExchange stateTopicExchange, Queue transactionStateQueue) {
    return BindingBuilder.bind(transactionStateQueue).to(stateTopicExchange).with(Topics.CATCH_ALL);
  }

  @Bean
  public Binding businessLogBinding(TopicExchange topicExchange, Queue businessLogQueue) {
    return BindingBuilder.bind(businessLogQueue)
        .to(topicExchange)
        .with(Topics.PERSONDATA_BUSINESS_VALIDATION);
  }

  @Bean
  public ConnectionFactory connectionFactory(
      @Value("${spring.rabbitmq.username:guest}") String username,
      @Value("${spring.rabbitmq.password:guest}") String password,
      @Value("${spring.rabbitmq.host:localhost}") String host,
      @Value("${spring.rabbitmq.port:5672}") int port,
      @Value("${spring.rabbitmq.virtual-host:/}") String vhost) {
    final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();

    connectionFactory.setUsername(username);
    connectionFactory.setPassword(password);
    connectionFactory.setHost(host);
    connectionFactory.setPort(port);
    connectionFactory.setVirtualHost(vhost);

    return connectionFactory;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setChannelTransacted(true);
    return rabbitTemplate;
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    return factory;
  }

  @Bean
  public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }
}
