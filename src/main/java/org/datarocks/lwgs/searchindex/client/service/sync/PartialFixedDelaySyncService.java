package org.datarocks.lwgs.searchindex.client.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class PartialFixedDelaySyncService extends AbstractSyncService {
  public PartialFixedDelaySyncService(RabbitTemplate template) {
    super(template);
  }

  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sync.partial.fixed-delay:1000}")
  public void fixedDelayPartial() {
    processQueue(JobType.PARTIAL, Queues.PERSONDATA_PARTIAL_OUTGOING, Topics.SEDEX_OUTBOX);
  }
}
