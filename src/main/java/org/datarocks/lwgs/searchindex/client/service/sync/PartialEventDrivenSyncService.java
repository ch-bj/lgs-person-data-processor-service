package org.datarocks.lwgs.searchindex.client.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.model.ProcessedPersonData;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
public class PartialEventDrivenSyncService extends AbstractSyncService {
  public PartialEventDrivenSyncService(RabbitTemplate template, int pageSize) {
    super(template, pageSize);
  }

  @RabbitListener(queues = Queues.PERSONDATA_PARTIAL_OUTGOING)
  public void listenPartial(ProcessedPersonData processedPersonData) {
    processEvent(JobType.PARTIAL, Topics.SEDEX_OUTBOX, processedPersonData);
  }
}
