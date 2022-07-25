package org.datarocks.lwgs.searchindex.client.service.sedex;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.commons.sedex.SedexFileWriter;
import org.datarocks.lwgs.commons.sedex.model.SedexEnvelope;
import org.datarocks.lwgs.searchindex.client.configuration.SedexConfiguration;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.model.JobCollectedPersonData;
import org.datarocks.lwgs.searchindex.client.service.amqp.*;
import org.datarocks.lwgs.searchindex.client.util.BinarySerializerUtil;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SedexFileWriterService {
  private final SedexFileWriter sedexFileWriter;
  private final RabbitTemplate rabbitTemplate;
  private boolean throttlingActive = false;
  private int errorCount = 0;
  private Instant retryTime = Instant.MIN;

  @Value("${lwgs.searchindex.client.sedex.file-writer.failure.throttling.base:1000}")
  private Long errorThrottlingBase;

  @Value("${lwgs.searchindex.client.sedex.file-writer.failure.throttling.max:600000}")
  private Long errorThrottlingMax;

  @Value("${lwgs.searchindex.client.sedex.sender-id}")
  private String sedexSenderId;

  @Value("${lwgs.searchindex.client.sedex.recipient-id}")
  private String sedexRecipientId;

  @Value("${lwgs.searchindex.client.sedex.message.type}")
  private int sedexMessageType;

  @Value("${lwgs.searchindex.client.sedex.message.class}")
  private int sedexMessageClass;

  @Autowired
  public SedexFileWriterService(
      SedexConfiguration sedexConfiguration, RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    this.sedexFileWriter =
        new SedexFileWriter(
            sedexConfiguration.getSedexOutboxPath(), sedexConfiguration.shouldCreateDirectories());
  }

  private void updateThrottling(boolean active) {
    if (active) {
      long waitingTime =
          Long.min((long) Math.pow(2, errorCount) * errorThrottlingBase, errorThrottlingMax);
      retryTime = Instant.now().plusMillis(waitingTime);
      errorCount++;
    } else {
      retryTime = Instant.MIN;
      errorCount = 0;
    }
    throttlingActive = active;
  }

  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sedex.file-writer.fixed-delay:1000}")
  @Async
  @Transactional
  public void processSedexOutbox() {
    boolean loop;

    if (throttlingActive) {
      if (Instant.now().isBefore(retryTime)) {
        log.info(
            "Processing queue failed {} times, skip sedexFileWriter runs until {} (T-{}sec)",
            errorCount,
            retryTime,
            Duration.between(Instant.now(), retryTime).getSeconds());
        return;
      }
      log.info("Retry processing sedexFileWriter.");
    }

    do {
      Message message = rabbitTemplate.receive(Queues.SEDEX_OUTBOX);
      loop = (message != null);
      if (loop) {
        log.info("Start processing queue " + Queues.SEDEX_OUTBOX);
        UUID fileIdentifier = UUID.randomUUID();
        try {
          final JobCollectedPersonData jobCollectedPersonData =
              BinarySerializerUtil.convertByteArrayToObject(
                  message.getBody(), JobCollectedPersonData.class);

          final CommonHeadersDao inHeaders =
              new CommonHeadersDao(message.getMessageProperties().getHeaders());

          final SedexEnvelope envelope =
              SedexEnvelope.builder()
                  .messageId(
                      inHeaders.getJobId().toString() + '-' + jobCollectedPersonData.getPage())
                  .messageType(sedexMessageType)
                  .messageClass(sedexMessageClass)
                  .senderId(sedexSenderId)
                  .recipientId(sedexRecipientId)
                  .eventDate(inHeaders.getTimestamp())
                  .messageDate(inHeaders.getTimestamp())
                  .build();

          sedexFileWriter.writeSedexPayload(fileIdentifier, jobCollectedPersonData);
          sedexFileWriter.writeSedexEnvelope(fileIdentifier, envelope);

          final CommonHeadersDao outHeaders =
              CommonHeadersDao.builder(inHeaders).jobState(JobState.SENT).timestamp().build();

          rabbitTemplate.convertAndSend(
              Exchanges.LWGS,
              Topics.SEDEX_SENT,
              envelope,
              outHeaders::applyAndSetJobIdAsCorrelationId);
        } catch (Exception e) {
          updateThrottling(true);
          log.error("Fatal error: " + e.getMessage());
          throw new ListenerExecutionFailedException("Writing Sedex file failed", e, message);
        }
      }
    } while (loop);
    updateThrottling(false);
  }
}
