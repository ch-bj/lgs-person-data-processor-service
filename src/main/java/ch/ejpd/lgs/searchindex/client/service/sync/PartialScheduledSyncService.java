package ch.ejpd.lgs.searchindex.client.service.sync;

import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.service.amqp.Topics;
import ch.ejpd.lgs.searchindex.client.util.SenderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class PartialScheduledSyncService extends AbstractSyncService {
  public PartialScheduledSyncService(RabbitTemplate template, int pageSize, SenderUtil senderUtil) {
    super(template, pageSize, senderUtil);
  }

  @Scheduled(cron = "${lwgs.searchindex.client.sync.partial.cron-schedule}")
  public void scheduledPartial() {
    processPartialQueue(Queues.PERSONDATA_PARTIAL_OUTGOING, Topics.SEDEX_OUTBOX);
  }
}
