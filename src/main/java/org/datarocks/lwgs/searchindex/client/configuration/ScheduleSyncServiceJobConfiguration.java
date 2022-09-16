package org.datarocks.lwgs.searchindex.client.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.datarocks.lwgs.searchindex.client.service.sync.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  public PartialEventDrivenSyncService partialEventDrivenSyncService(
      @Value("${lwgs.searchindex.client.sync.partial.page-size:5000}") int pageSize) {
    return new PartialEventDrivenSyncService(template, pageSize);
  }

  @Bean
  @ConditionalOnProperty(
      value = "lwgs.searchindex.client.sync.partial.scheduling-type",
      havingValue = "FIXED_DELAY")
  public PartialFixedDelaySyncService partialFixedDelaySyncService(
      @Value("${lwgs.searchindex.client.sync.partial.page-size:5000}") int pageSize) {
    return new PartialFixedDelaySyncService(template, pageSize);
  }

  @Bean
  @ConditionalOnProperty(
      value = "lwgs.searchindex.client.sync.partial.scheduling-type",
      havingValue = "CRON_SCHEDULE")
  public PartialScheduledSyncService partialScheduledSyncService(
      @Value("${lwgs.searchindex.client.sync.partial.page-size:5000}") int pageSize) {
    return new PartialScheduledSyncService(template, pageSize);
  }

  @Bean
  public FullSyncService fullSyncService(
      @Value("${lwgs.searchindex.client.sync.full.page-size:5000}") int pageSize) {
    return new FullSyncService(template, fullSyncStateManager, pageSize);
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
