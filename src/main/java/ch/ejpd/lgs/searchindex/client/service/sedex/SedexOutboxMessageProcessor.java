package ch.ejpd.lgs.searchindex.client.service.sedex;

import ch.ejpd.lgs.commons.sedex.SedexFileWriter;
import ch.ejpd.lgs.commons.sedex.model.SedexEnvelope;
import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.entity.SedexMessage;
import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.entity.type.SedexMessageState;
import ch.ejpd.lgs.searchindex.client.model.JobCollectedPersonData;
import ch.ejpd.lgs.searchindex.client.model.JobMetaData;
import ch.ejpd.lgs.searchindex.client.repository.SedexMessageRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.Exchanges;
import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.service.amqp.Topics;
import ch.ejpd.lgs.searchindex.client.util.BinarySerializerUtil;
import java.time.Instant;
import java.util.Date;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class SedexOutboxMessageProcessor {
  private final RabbitTemplate rabbitTemplate;
  private final SedexConfiguration configuration;
  private final SedexMessageRepository sedexMessageRepository;
  private final ThrottleHandler throttleHandler;

  public SedexOutboxMessageProcessor(
      @NonNull final RabbitTemplate rabbitTemplate,
      @NonNull final SedexConfiguration configuration,
      @NonNull final SedexMessageRepository sedexMessageRepository,
      @NonNull final ThrottleHandler throttleHandler) {
    this.rabbitTemplate = rabbitTemplate;
    this.configuration = configuration;
    this.sedexMessageRepository = sedexMessageRepository;
    this.throttleHandler = throttleHandler;
  }

  @Transactional
  public boolean processNextSedexOutboxMessage() {
    final Message message = rabbitTemplate.receive(Queues.SEDEX_OUTBOX);

    if (message == null) {
      return false;
    }

    try {
      final JobCollectedPersonData jobCollectedPersonData =
          BinarySerializerUtil.convertByteArrayToObject(
              message.getBody(), JobCollectedPersonData.class);

      final CommonHeadersDao inHeaders =
          new CommonHeadersDao(message.getMessageProperties().getHeaders());

      log.info(
          "Start processing queue next message in {} [messageId: {}, senderId: {}]",
          Queues.SEDEX_OUTBOX,
          jobCollectedPersonData.getMessageId(),
          inHeaders.getSenderId());

      final SedexFileWriter sedexFileWriter =
          configuration.isInMultiSenderMode()
              ? new SedexFileWriter(
                  configuration.getSedexOutboxPath(inHeaders.getSenderId()),
                  configuration.shouldCreateDirectories())
              : new SedexFileWriter(
                  configuration.getSedexOutboxPath(), configuration.shouldCreateDirectories());

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
              inHeaders.getSenderId(),
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
          JobMetaData.builder()
              .type(inHeaders.getJobType())
              .jobId(inHeaders.getJobId())
              .pageNr(jobCollectedPersonData.getPage())
              .isLastPage(isLastPage)
              .build();

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
