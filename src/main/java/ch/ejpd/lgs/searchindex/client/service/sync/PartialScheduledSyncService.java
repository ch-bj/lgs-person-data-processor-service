package ch.ejpd.lgs.searchindex.client.service.sync;

import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.service.amqp.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Service class for partial synchronization using a scheduled cron job.
 * Extends AbstractSyncService to inherit common synchronization functionality.
 */
@Slf4j
public class PartialScheduledSyncService extends AbstractSyncService {

  /**
   * Constructor for PartialScheduledSyncService.
   *
   * @param template RabbitTemplate for interacting with RabbitMQ.
   * @param pageSize The size of pages to process during synchronization.
   */
  public PartialScheduledSyncService(RabbitTemplate template, int pageSize) {
    super(template, pageSize);
  }

  /**
   * Scheduled method to perform partial synchronization based on a cron schedule.
   * The cron expression is configured in the application properties.
   */
  @Scheduled(cron = "${lwgs.searchindex.client.sync.partial.cron-schedule}")
  public void scheduledPartial() {
    processPartialQueue(Queues.PERSONDATA_PARTIAL_OUTGOING, Topics.SEDEX_OUTBOX);
  }
}
