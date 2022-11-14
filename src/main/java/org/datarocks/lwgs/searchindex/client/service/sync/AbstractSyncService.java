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
  private final int pageSize;

  public AbstractSyncService(@NonNull RabbitTemplate rabbitTemplate, int pageSize) {
    this.rabbitTemplate = rabbitTemplate;
    this.pageSize = pageSize;
  }

  public void processQueue(
      @NonNull final JobType jobType,
      @NonNull final String inQueueName,
      @NonNull final String outTopicName) {
    processQueue(jobType, inQueueName, outTopicName, null);
  }

  public int processQueuePage(
      @NonNull final JobType jobType,
      @NonNull final String inQueueName,
      @NonNull final String outTopicName,
      final UUID currentJobId,
      final int page,
      final int numProcessed,
      final int numTotal) {
    log.debug("Start processing queue {}, page: {}.", inQueueName, page);

    final UUID jobId = currentJobId != null ? currentJobId : UUID.randomUUID();
    try (Connection connection = rabbitTemplate.getConnectionFactory().createConnection()) {

      try (Channel channel = connection.createChannel(TRANSACTIONAL)) {
        channel.txSelect();
        final List<ProcessedPersonData> processedPersonDataList =
            getMessagesUntilPageFullOrQueueIsEmpty(channel, inQueueName);

        if (processedPersonDataList.isEmpty()) {
          channel.txCommit(); // Commit rejected messages
          log.debug("Nothing to process. Returning.");
          return 0;
        }

        final JobCollectedPersonData jobCollectedPersonData =
            JobCollectedPersonData.builder()
                .jobId(jobId)
                .page(page)
                .numProcessed(numProcessed + processedPersonDataList.size())
                .numTotal(numTotal)
                .processedPersonDataList(processedPersonDataList)
                .build();

        log.info(
            "Sending paged transactions to topic {}. [jobId:{}; page:{}; numTransactions:{}; "
                + "numProcessed: {}; numTotal: {}]",
            outTopicName,
            jobId,
            page,
            processedPersonDataList.size(),
            numProcessed + processedPersonDataList.size(),
            numTotal);
        try {
          final byte[] byteProcessedSedexPersonData =
              BinarySerializerUtil.convertObjectToByteArray(jobCollectedPersonData);

          final CommonHeadersDao headersDao =
              CommonHeadersDao.builder()
                  .messageCategory(MessageCategory.JOB_EVENT)
                  .jobState(JobState.NEW)
                  .jobId(jobCollectedPersonData.getJobId())
                  .jobType(jobType)
                  .timestamp(Instant.now())
                  .build();

          final BasicProperties properties =
              (new BasicProperties.Builder())
                  .headers(headersDao.toMap())
                  .correlationId(jobCollectedPersonData.getJobId().toString())
                  .build();

          channel.basicPublish(
              Exchanges.LWGS, outTopicName, properties, byteProcessedSedexPersonData);

          channel.txCommit();

          return processedPersonDataList.size();

        } catch (SerializationFailedException e) {
          log.warn(
              "Serialization of JobCollectedPersonData failed. Rolling back all transactions[{}]",
              getTransactionIds(processedPersonDataList));
          channel.txRollback();
        }
      } catch (TimeoutException e) {
        log.warn(
            "TimeoutException when processing channel for queue {}. Stop processing.", inQueueName);
      } catch (IOException e) {
        log.warn(
            "IOException when processing channel for queue {}}. Stop processing.", inQueueName);
      }
      return 0;
    }
  }

  public void processQueue(
      @NonNull final JobType jobType,
      @NonNull final String inQueueName,
      @NonNull final String outTopicName,
      final UUID currentJobId) {
    log.debug("Start processing queue " + inQueueName + ".");
    int page = 0;
    final UUID jobId = currentJobId != null ? currentJobId : UUID.randomUUID();
    try (Connection connection = rabbitTemplate.getConnectionFactory().createConnection()) {

      while (true) {
        try (Channel channel = connection.createChannel(TRANSACTIONAL)) {
          channel.txSelect();
          final List<ProcessedPersonData> processedPersonDataList =
              getMessagesUntilPageFullOrQueueIsEmpty(channel, inQueueName);

          if (processedPersonDataList.isEmpty()) {
            channel.txCommit(); // Commit rejected messages
            log.debug("Nothing to process. Returning.");
            return;
          }

          final JobCollectedPersonData jobCollectedPersonData =
              JobCollectedPersonData.builder()
                  .jobId(jobId)
                  .page(page)
                  .processedPersonDataList(processedPersonDataList)
                  .build();

          log.info(
              "Sending transactions to sedex outbox. [jobId:{}; page:{}; numTransactions:{}]",
              jobId,
              page,
              processedPersonDataList.size());
          try {
            final byte[] byteProcessedSedexPersonData =
                BinarySerializerUtil.convertObjectToByteArray(jobCollectedPersonData);

            final CommonHeadersDao headersDao =
                CommonHeadersDao.builder()
                    .messageCategory(MessageCategory.JOB_EVENT)
                    .jobState(JobState.NEW)
                    .jobId(jobCollectedPersonData.getJobId())
                    .jobType(jobType)
                    .timestamp(Instant.now())
                    .build();

            final BasicProperties properties =
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
          page++;
        } catch (TimeoutException e) {
          log.warn(
              "TimeoutException when processing channel for queue "
                  + inQueueName
                  + ". Stop processing.");
        } catch (IOException e) {
          log.warn(
              "IOException when processing channel for queue "
                  + inQueueName
                  + ". Stop processing.");
        }
      }
    }
  }

  private List<UUID> getTransactionIds(List<ProcessedPersonData> processedPersonDataList) {
    return processedPersonDataList.stream()
        .map(ProcessedPersonData::getTransactionId)
        .collect(Collectors.toList());
  }

  private List<ProcessedPersonData> getMessagesUntilPageFullOrQueueIsEmpty(
      Channel channel, String inQueueName) throws IOException {
    final List<ProcessedPersonData> processedPersonDataList = new ArrayList<>();
    boolean loop;
    int count = 0;
    do {
      final GetResponse response = channel.basicGet(inQueueName, false);

      loop = (response != null);

      if (loop) {
        try {

          final ProcessedPersonData processedPersonData =
              BinarySerializerUtil.convertByteArrayToObject(
                  response.getBody(), ProcessedPersonData.class);

          processedPersonDataList.add(processedPersonData);

          channel.basicAck(response.getEnvelope().getDeliveryTag(), ACK_SINGLE_MESSAGE);
          count++;
          log.debug(
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
    } while (loop && count < pageSize);
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
