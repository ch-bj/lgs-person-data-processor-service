package org.datarocks.lwgs.searchindex.client.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class PartialScheduledSyncService extends AbstractSyncService {
  public PartialScheduledSyncService(RabbitTemplate template) {
    super(template);
  }

  @Scheduled(cron = "${lwgs.searchindex.client.sync.partial.cron-schedule}")
  public void scheduledPartial() {
    processQueue(JobType.PARTIAL, Queues.PERSONDATA_PARTIAL_OUTGOING, Topics.SEDEX_OUTBOX);
  }
}
