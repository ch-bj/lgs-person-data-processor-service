package org.datarocks.lwgs.searchindex.client.service.sedex;

import static org.datarocks.lwgs.commons.sedex.model.SedexStatusCategory.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.commons.filewatcher.FileEvent;
import org.datarocks.lwgs.commons.sedex.SedexReceiptReader;
import org.datarocks.lwgs.commons.sedex.model.SedexReceipt;
import org.datarocks.lwgs.commons.sedex.model.SedexStatus;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;
import org.datarocks.lwgs.searchindex.client.service.amqp.*;
import org.datarocks.lwgs.searchindex.client.service.log.Logger;
import org.datarocks.lwgs.searchindex.client.service.log.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SedexReceiptProcessor {
  private final Logger lwgsLogger;
  private final SedexReceiptReader receiptReader;
  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public SedexReceiptProcessor(LoggerFactory loggerFactory, RabbitTemplate rabbitTemplate) {
    this.lwgsLogger = loggerFactory.getLogger(SourceType.SEDEX_HANDLER);
    this.rabbitTemplate = rabbitTemplate;
    this.receiptReader = new SedexReceiptReader();
  }

  @RabbitListener(queues = Queues.SEDEX_RECEIPTS)
  public void listen(FileEvent event) {

    Optional<SedexReceipt> optionalReceipt =
        receiptReader.readFromFile(Paths.get(event.getFilename()));

    if (optionalReceipt.isEmpty()) {
      log.warn("Invalid receipt");
      lwgsLogger.error("Received invalid sedex receipt.");

      return;
    }

    final SedexReceipt receipt = optionalReceipt.get();
    final SedexStatus status = SedexStatus.valueOf(receipt.getStatusCode());

    // TODO: handle multi page requests, right now one delivered message makes jobs a success

    if (status.getCategory() == SUCCESS) {
      final CommonHeadersDao headers =
          CommonHeadersDao.builder()
              .messageCategory(MessageCategory.JOB_EVENT)
              .jobId(UUID.fromString(receipt.getMessageId()))
              .jobState(JobState.COMPLETED)
              .timestamp(receipt.getEventDate())
              .build();

      rabbitTemplate.convertAndSend(
          Exchanges.LWGS, Topics.SEDEX_RECEIVED, receipt, headers::applyAndSetJobIdAsCorrelationId);
    } else if (Arrays.asList(MESSAGE_ERROR, AUTHORIZATION_ERROR, ADAPTER_ERROR, TRANSPORT_ERROR)
        .contains(status.getCategory())) {
      final CommonHeadersDao headers =
          CommonHeadersDao.builder()
              .messageCategory(MessageCategory.JOB_EVENT)
              .jobId(UUID.fromString(receipt.getMessageId()))
              .jobState(JobState.FAILED)
              .timestamp(receipt.getEventDate())
              .build();
      rabbitTemplate.convertAndSend(
          Exchanges.LWGS, Topics.SEDEX_RECEIVED, receipt, headers::applyAndSetJobIdAsCorrelationId);
    }
  }
}
