package org.datarocks.lwgs.searchindex.client.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.datarocks.lwgs.searchindex.client.service.sync.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class ScheduleSyncServiceJobConfiguration implements SchedulingConfigurer {
  private final RabbitTemplate template;
  private final FullSyncStateManager fullSyncStateManager;

  @Autowired
  public ScheduleSyncServiceJobConfiguration(
      RabbitTemplate template, FullSyncStateManager fullSyncStateManager) {
    this.template = template;
    this.fullSyncStateManager = fullSyncStateManager;
  }

  @Bean
  @ConditionalOnProperty(
      value = "lwgs.searchindex.client.sync.partial.scheduling-type",
      havingValue = "EVENT_DRIVEN")
  public PartialEventDrivenSyncService partialEventDrivenSyncService() {
    return new PartialEventDrivenSyncService(template);
  }

  @Bean
  @ConditionalOnProperty(
      value = "lwgs.searchindex.client.sync.partial.scheduling-type",
      havingValue = "FIXED_DELAY")
  public PartialFixedDelaySyncService partialFixedDelaySyncService() {
    return new PartialFixedDelaySyncService(template);
  }

  @Bean
  @ConditionalOnProperty(
      value = "lwgs.searchindex.client.sync.partial.scheduling-type",
      havingValue = "CRON_SCHEDULE")
  public PartialScheduledSyncService partialScheduledSyncService() {
    return new PartialScheduledSyncService(template);
  }

  @Bean
  public FullSyncService fullSyncService() {
    return new FullSyncService(template, fullSyncStateManager);
  }

  @Bean(destroyMethod = "shutdown")
  public Executor taskExecutor() {
    return Executors.newScheduledThreadPool(20);
  }

  @Override
  public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
    scheduledTaskRegistrar.setScheduler(taskExecutor());
  }
}
