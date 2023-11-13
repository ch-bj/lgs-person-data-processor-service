package ch.ejpd.lgs.searchindex.client.service.sync;

import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.service.amqp.Topics;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * Service class for handling partial event-driven synchronization of processed person data.
 * Extends AbstractSyncService to leverage common synchronization functionality.
 */
@Slf4j
public class PartialEventDrivenSyncService extends AbstractSyncService {

  /**
   * Constructor for PartialEventDrivenSyncService.
   *
   * @param template  RabbitTemplate for interacting with RabbitMQ.
   * @param pageSize  The size of pages to process during synchronization.
   */
  public PartialEventDrivenSyncService(RabbitTemplate template, int pageSize) {
    super(template, pageSize);
  }

  /**
   * RabbitMQ listener method for handling incoming messages from the PERSONDATA_PARTIAL_OUTGOING queue.
   * Invoked when a new message is received.
   *
   * @param processedPersonData Processed person data received in the message payload.
   * @param rawHeaders           Raw headers received with the message.
   */
  @RabbitListener(queues = Queues.PERSONDATA_PARTIAL_OUTGOING)
  public void listenPartial(
      @Payload ProcessedPersonData processedPersonData, @Headers Map<String, Object> rawHeaders) {
    final CommonHeadersDao headers = new CommonHeadersDao(rawHeaders);
    processEvent(JobType.PARTIAL, Topics.SEDEX_OUTBOX, processedPersonData, headers.getSenderId());
  }
}
