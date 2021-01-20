package org.datarocks.lwgs.searchindex.client.service.sync;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.model.JobCollectedPersonData;
import org.datarocks.lwgs.searchindex.client.model.ProcessedPersonData;
import org.datarocks.lwgs.searchindex.client.service.amqp.CommonHeadersDao;
import org.datarocks.lwgs.searchindex.client.service.amqp.Exchanges;
import org.datarocks.lwgs.searchindex.client.service.amqp.MessageCategory;
import org.datarocks.lwgs.searchindex.client.service.exception.DeserializationFailedException;
import org.datarocks.lwgs.searchindex.client.service.exception.SerializationFailedException;
import org.datarocks.lwgs.searchindex.client.util.BinarySerializerUtil;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
public abstract class AbstractSyncService {
  private static final boolean ACK_SINGLE_MESSAGE = false;
  private static final boolean REJECT_DO_NOT_REQUEUE = false;
  private static final boolean REJECT_SINGLE_MESSAGE = false;
  private static final boolean TRANSACTIONAL = true;

  private final RabbitTemplate rabbitTemplate;

  public AbstractSyncService(@NonNull RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void processQueue(
      @NonNull final JobType jobType,
      @NonNull final String inQueueName,
      @NonNull final String outTopicName) {
    log.info("Start processing queue " + inQueueName + ".");

    try (Connection connection = rabbitTemplate.getConnectionFactory().createConnection()) {

      try (Channel channel = connection.createChannel(TRANSACTIONAL)) {
        channel.txSelect();

        final List<ProcessedPersonData> processedPersonDataList =
            getMessagesUntilQueueIsEmpty(channel, inQueueName);

        if (processedPersonDataList.isEmpty()) {
          channel.txCommit(); // Commit rejected messages
          log.info("Nothing to process. Returning.");
          return;
        }

        JobCollectedPersonData jobCollectedPersonData =
            JobCollectedPersonData.builder()
                .jobId(UUID.randomUUID())
                .processedPersonDataList(processedPersonDataList)
                .build();

        log.info(
            "Sending transactions to sedex outbox. TransactionIds["
                + getTransactionIds(processedPersonDataList).toString()
                + "]");
        try {
          byte[] byteProcessedSedexPersonData =
              BinarySerializerUtil.convertObjectToByteArray(jobCollectedPersonData);

          final CommonHeadersDao headersDao =
              CommonHeadersDao.builder()
                  .messageCategory(MessageCategory.JOB_EVENT)
                  .jobState(JobState.NEW)
                  .jobId(jobCollectedPersonData.getJobId())
                  .jobType(jobType)
                  .timestamp(Instant.now())
                  .build();

          BasicProperties properties =
              (new BasicProperties.Builder())
                  .headers(headersDao.toMap())
                  .correlationId(jobCollectedPersonData.getJobId().toString())
                  .build();

          channel.basicPublish(
              Exchanges.LWGS, outTopicName, properties, byteProcessedSedexPersonData);

          channel.txCommit();
        } catch (SerializationFailedException e) {
          log.warn(
              "Serialization of JobCollectedPersonData failed. Rolling back all transactions["
                  + getTransactionIds(processedPersonDataList).toString()
                  + "]");
          channel.txRollback();
        }
      } catch (TimeoutException e) {
        log.warn(
            "TimeoutException when processing channel for queue "
                + inQueueName
                + ". Stop processing.");
      } catch (IOException e) {
        log.warn(
            "IOException when processing channel for queue " + inQueueName + ". Stop processing.");
      }
    }
    log.info("DONEDONEDONE");
  }

  private List<UUID> getTransactionIds(List<ProcessedPersonData> processedPersonDataList) {
    return processedPersonDataList.stream()
        .map(ProcessedPersonData::getTransactionId)
        .collect(Collectors.toList());
  }

  private List<ProcessedPersonData> getMessagesUntilQueueIsEmpty(
      Channel channel, String inQueueName) throws IOException {
    final List<ProcessedPersonData> processedPersonDataList = new ArrayList<>();
    boolean loop;
    do {
      GetResponse response = channel.basicGet(inQueueName, false);

      loop = (response != null);

      if (loop) {
        try {

          ProcessedPersonData processedPersonData =
              BinarySerializerUtil.convertByteArrayToObject(
                  response.getBody(), ProcessedPersonData.class);

          processedPersonDataList.add(processedPersonData);

          channel.basicAck(response.getEnvelope().getDeliveryTag(), ACK_SINGLE_MESSAGE);
          log.info(
              "Processing PersonData with transactionId: "
                  + processedPersonData.getTransactionId());
        } catch (DeserializationFailedException e) {
          // Deserialization failed. This is an unrecoverable exception. reject message and send to
          // failure queue.
          log.warn(
              "Processing of PersonData failed due to a deserialization error. Rejecting message.");

          channel.basicNack(
              response.getEnvelope().getDeliveryTag(),
              REJECT_SINGLE_MESSAGE,
              REJECT_DO_NOT_REQUEUE);
        }
      }
    } while (loop);
    return processedPersonDataList;
  }

  public void processEvent(
      @NonNull final JobType jobType,
      @NonNull final String outTopicName,
      @NonNull final ProcessedPersonData processedPersonData) {
    out(
        jobType,
        outTopicName,
        JobCollectedPersonData.builder()
            .jobId(UUID.randomUUID())
            .processedPersonDataList(Collections.singletonList(processedPersonData))
            .build());
  }

  private void out(
      JobType jobType, String topicName, JobCollectedPersonData jobCollectedPersonData) {

    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.JOB_EVENT)
            .jobId(jobCollectedPersonData.getJobId())
            .jobType(jobType)
            .jobState(JobState.NEW)
            .timestamp()
            .build();

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS,
        topicName,
        jobCollectedPersonData,
        headers::applyAndSetJobIdAsCorrelationId);
  }
}
