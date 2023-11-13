package ch.ejpd.lgs.searchindex.client.service.sync;

import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.service.amqp.Topics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Service class for partial synchronization with a fixed delay.
 * Extends AbstractSyncService to inherit common synchronization functionality.
 */
@Slf4j
public class PartialFixedDelaySyncService extends AbstractSyncService {

  /**
   * Constructor for PartialFixedDelaySyncService.
   *
   * @param template RabbitTemplate for interacting with RabbitMQ.
   * @param pageSize The size of pages to process during synchronization.
   */
  public PartialFixedDelaySyncService(RabbitTemplate template, int pageSize) {
    super(template, pageSize);
  }

  /**
   * Scheduled method to perform partial synchronization with a fixed delay.
   * Configured with a fixed delay retrieved from the application properties.
   */
  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sync.partial.fixed-delay:300000}")
  public void fixedDelayPartial() {
    processPartialQueue(Queues.PERSONDATA_PARTIAL_OUTGOING, Topics.SEDEX_OUTBOX);
  }
}
