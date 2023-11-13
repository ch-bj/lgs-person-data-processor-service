package ch.ejpd.lgs.searchindex.client.service.sedex;

import static ch.ejpd.lgs.commons.sedex.model.SedexStatusCategory.*;

import ch.ejpd.lgs.commons.filewatcher.FileEvent;
import ch.ejpd.lgs.commons.sedex.SedexReceiptReader;
import ch.ejpd.lgs.commons.sedex.model.SedexReceipt;
import ch.ejpd.lgs.commons.sedex.model.SedexStatus;
import ch.ejpd.lgs.searchindex.client.entity.SedexMessage;
import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.SedexMessageState;
import ch.ejpd.lgs.searchindex.client.repository.SedexMessageRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.*;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processor for handling Sedex receipts and updating Sedex message states.
 */
@Component
@Slf4j
public class SedexReceiptProcessor {
  private final SedexReceiptReader receiptReader;
  private final RabbitTemplate rabbitTemplate;
  private final SedexMessageRepository sedexMessageRepository;

  @Autowired
  public SedexReceiptProcessor(
      RabbitTemplate rabbitTemplate, SedexMessageRepository sedexMessageRepository) {
    this.rabbitTemplate = rabbitTemplate;
    this.sedexMessageRepository = sedexMessageRepository;
    this.receiptReader = new SedexReceiptReader();
  }

  /**
   * Listen for Sedex receipt messages and update Sedex message states accordingly.
   *
   * @param event File event representing the receipt file.
   */
  @RabbitListener(queues = Queues.SEDEX_RECEIPTS)
  @Transactional
  public void listen(FileEvent event) {

    final Optional<SedexReceipt> optionalReceipt =
        receiptReader.readFromFile(Paths.get(event.getFilename()));

    if (optionalReceipt.isEmpty()) {
      log.warn("Invalid receipt");

      return;
    }

    final SedexReceipt receipt = optionalReceipt.get();
    final SedexStatus status = SedexStatus.valueOf(receipt.getStatusCode());
    final Optional<SedexMessage> optionalSedexMessage =
        sedexMessageRepository.findBySedexMessageId(UUID.fromString(receipt.getMessageId()));

    if (optionalSedexMessage.isEmpty()) {
      log.warn("No matching sedexMessage found for receipt with id: {}.", receipt.getMessageId());

      return;
    }

    final SedexMessage message = optionalSedexMessage.get();

    // Update Sedex message state based on receipt status category
    if (status.getCategory() == SUCCESS) {
      message.setState(SedexMessageState.SUCCESSFUL);
      message.setUpdatedAt(Date.from(Instant.now()));
      sedexMessageRepository.save(message);

      // Send a JOB_EVENT with COMPLETED state to Sedex status updated topic
      final CommonHeadersDao headers =
          CommonHeadersDao.builder()
              .messageCategory(MessageCategory.JOB_EVENT)
              .senderId(message.getSenderId())
              .jobId(message.getJobId())
              .jobState(JobState.COMPLETED)
              .timestamp(receipt.getEventDate())
              .build();

      rabbitTemplate.convertAndSend(
          Exchanges.LWGS,
          Topics.SEDEX_STATUS_UPDATED,
          receipt,
          headers::applyAndSetJobIdAsCorrelationId);
    } else if (Arrays.asList(MESSAGE_ERROR, AUTHORIZATION_ERROR, ADAPTER_ERROR, TRANSPORT_ERROR)
        .contains(status.getCategory())) {
      message.setState(SedexMessageState.FAILED);
      message.setUpdatedAt(Date.from(Instant.now()));
      sedexMessageRepository.save(message);

      // Send a JOB_EVENT with FAILED state to Sedex status updated topic
      final CommonHeadersDao headers =
          CommonHeadersDao.builder()
              .messageCategory(MessageCategory.JOB_EVENT)
              .senderId(message.getSenderId())
              .jobId(message.getJobId())
              .jobState(JobState.FAILED)
              .timestamp(receipt.getEventDate())
              .build();

      rabbitTemplate.convertAndSend(
          Exchanges.LWGS,
          Topics.SEDEX_STATUS_UPDATED,
          receipt,
          headers::applyAndSetJobIdAsCorrelationId);
    }
  }
}
