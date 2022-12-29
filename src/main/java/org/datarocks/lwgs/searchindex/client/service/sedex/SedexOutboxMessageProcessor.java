package org.datarocks.lwgs.searchindex.client.service.sedex;

import java.time.Instant;
import java.util.Date;
import lombok.NonNull;
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
import org.datarocks.lwgs.searchindex.client.service.amqp.CommonHeadersDao;
import org.datarocks.lwgs.searchindex.client.service.amqp.Exchanges;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.datarocks.lwgs.searchindex.client.util.BinarySerializerUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class SedexOutboxMessageProcessor {
  private final RabbitTemplate rabbitTemplate;
  private final SedexConfiguration configuration;
  private final SedexMessageRepository sedexMessageRepository;
  private final SedexFileWriter sedexFileWriter;
  private final ThrottleHandler throttleHandler;

  public SedexOutboxMessageProcessor(
      @NonNull final RabbitTemplate rabbitTemplate,
      @NonNull final SedexConfiguration configuration,
      @NonNull final SedexMessageRepository sedexMessageRepository,
      @NonNull final SedexFileWriter sedexFileWriter,
      @NonNull final ThrottleHandler throttleHandler) {
    this.rabbitTemplate = rabbitTemplate;
    this.configuration = configuration;
    this.sedexMessageRepository = sedexMessageRepository;
    this.sedexFileWriter = sedexFileWriter;
    this.throttleHandler = throttleHandler;
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
              .senderId(jobCollectedPersonData.getSenderId())
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
      throttleHandler.updateThrottling(true);
      log.error("Fatal error: " + e.getMessage());
      throw new ListenerExecutionFailedException("Writing Sedex file failed", e, message);
    }
    return true;
  }
}
