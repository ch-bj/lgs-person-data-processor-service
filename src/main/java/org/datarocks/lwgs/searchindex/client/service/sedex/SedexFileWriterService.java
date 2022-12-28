package org.datarocks.lwgs.searchindex.client.service.sedex;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.commons.sedex.SedexFileWriter;
import org.datarocks.lwgs.commons.sedex.model.SedexEnvelope;
import org.datarocks.lwgs.searchindex.client.configuration.SedexConfiguration;
import org.datarocks.lwgs.searchindex.client.entity.SedexMessage;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.entity.type.SedexMessageState;
import org.datarocks.lwgs.searchindex.client.model.JobCollectedPersonData;
import org.datarocks.lwgs.searchindex.client.model.JobMetaData;
import org.datarocks.lwgs.searchindex.client.repository.SedexMessageRepository;
import org.datarocks.lwgs.searchindex.client.service.amqp.*;
import org.datarocks.lwgs.searchindex.client.util.BinarySerializerUtil;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SedexFileWriterService {
  private final SedexConfiguration configuration;
  private final SedexFileWriter sedexFileWriter;
  private final RabbitTemplate rabbitTemplate;

  private final SedexMessageRepository sedexMessageRepository;

  private boolean throttlingActive = false;
  private int errorCount = 0;
  private Instant retryTime = Instant.MIN;

  @Autowired
  public SedexFileWriterService(
      SedexConfiguration sedexConfiguration,
      RabbitTemplate rabbitTemplate,
      SedexMessageRepository sedexMessageRepository) {
    this.configuration = sedexConfiguration;
    this.rabbitTemplate = rabbitTemplate;
    this.sedexFileWriter =
        new SedexFileWriter(
            sedexConfiguration.getSedexOutboxPath(), sedexConfiguration.shouldCreateDirectories());
    this.sedexMessageRepository = sedexMessageRepository;
  }

  private void updateThrottling(boolean active) {
    if (active) {
      long waitingTime =
          Long.min(
              (long) Math.pow(2, errorCount) * configuration.getErrorThrottlingBase(),
              configuration.getErrorThrottlingMax());
      retryTime = Instant.now().plusMillis(waitingTime);
      errorCount++;
    } else {
      retryTime = Instant.MIN;
      errorCount = 0;
    }
    throttlingActive = active;
  }

  @Transactional
  public boolean processNextSedexOutboxMessage() {
    final Message message = rabbitTemplate.receive(Queues.SEDEX_OUTBOX);

    if (message == null) {
      return false;
    }

    log.info("Start processing queue " + Queues.SEDEX_OUTBOX);
    try {
      final JobCollectedPersonData jobCollectedPersonData =
          BinarySerializerUtil.convertByteArrayToObject(
              message.getBody(), JobCollectedPersonData.class);

      final CommonHeadersDao inHeaders =
          new CommonHeadersDao(message.getMessageProperties().getHeaders());

      final int sedexMessageType =
          (inHeaders.getJobType() == JobType.FULL)
              ? configuration.getSedexMessageTypeFullExport()
              : configuration.getSedexMessageTypeIncremental();

      final boolean isLastPage =
          inHeaders.getJobType() == JobType.PARTIAL
              || (jobCollectedPersonData.getNumProcessed() == jobCollectedPersonData.getNumTotal()
                  && jobCollectedPersonData.getNumTotal() > 0);

      sedexMessageRepository.save(
          new SedexMessage(
              jobCollectedPersonData.getMessageId(),
              Date.from(Instant.now()),
              Date.from(Instant.now()),
              SedexMessageState.CREATED,
              jobCollectedPersonData.getPage(),
              isLastPage,
              inHeaders.getJobType(),
              inHeaders.getJobId()));

      final SedexEnvelope envelope =
          SedexEnvelope.builder()
              .messageId(jobCollectedPersonData.getMessageId().toString())
              .messageType(sedexMessageType)
              .messageClass(configuration.getSedexMessageClass())
              .senderId(configuration.getSedexSenderId())
              .recipientId(configuration.getSedexRecipientId())
              .eventDate(inHeaders.getTimestamp())
              .messageDate(inHeaders.getTimestamp())
              .build();

      final JobMetaData metaData =
          new JobMetaData(
              inHeaders.getJobType(),
              inHeaders.getJobId(),
              jobCollectedPersonData.getPage(),
              isLastPage);

      sedexFileWriter.writeSedexPayload(
          jobCollectedPersonData.getMessageId(), jobCollectedPersonData, metaData);
      sedexFileWriter.writeSedexEnvelope(jobCollectedPersonData.getMessageId(), envelope);

      final CommonHeadersDao outHeaders =
          CommonHeadersDao.builder(inHeaders).jobState(JobState.SENT).timestamp().build();

      rabbitTemplate.convertAndSend(
          Exchanges.LWGS, Topics.SEDEX_SENT, envelope, outHeaders::applyAndSetJobIdAsCorrelationId);
    } catch (Exception e) {
      updateThrottling(true);
      log.error("Fatal error: " + e.getMessage());
      throw new ListenerExecutionFailedException("Writing Sedex file failed", e, message);
    }
    return true;
  }

  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sedex.file-writer.fixed-delay:1000}")
  @Async
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
      loop = processNextSedexOutboxMessage();
    } while (loop);

    updateThrottling(false);
  }
}
