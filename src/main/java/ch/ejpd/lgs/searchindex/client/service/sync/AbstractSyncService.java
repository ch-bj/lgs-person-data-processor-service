package ch.ejpd.lgs.searchindex.client.service.sync;

import static java.util.stream.Collectors.groupingBy;

import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.model.JobCollectedPersonData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.Exchanges;
import ch.ejpd.lgs.searchindex.client.service.amqp.MessageCategory;
import ch.ejpd.lgs.searchindex.client.service.exception.DeserializationFailedException;
import ch.ejpd.lgs.searchindex.client.service.exception.SerializationFailedException;
import ch.ejpd.lgs.searchindex.client.util.BinarySerializerUtil;
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

  protected AbstractSyncService(@NonNull RabbitTemplate rabbitTemplate, int pageSize) {
    this.rabbitTemplate = rabbitTemplate;
    this.pageSize = pageSize;
  }

  public int processFullQueuePaging(
      @NonNull final String inQueueName,
      @NonNull final String outTopicName,
      @NonNull final String senderId,
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
                .senderId(senderId)
                .jobId(jobId)
                .messageId(UUID.randomUUID())
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
                  .senderId(senderId)
                  .messageCategory(MessageCategory.JOB_EVENT)
                  .jobState(JobState.NEW)
                  .jobId(jobCollectedPersonData.getJobId())
                  .jobType(JobType.FULL)
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

  private void sendPartialMessage(
      @NonNull Channel channel,
      @NonNull String outTopicName,
      @NonNull String senderId,
      @NonNull List<ProcessedPersonData> processedPersonDataList)
      throws SerializationFailedException, IOException {
    final UUID messageId = UUID.randomUUID();
    final JobCollectedPersonData jobCollectedPersonData =
        JobCollectedPersonData.builder()
            .senderId(senderId)
            .jobId(messageId)
            .messageId(messageId)
            .page(0)
            .processedPersonDataList(processedPersonDataList)
            .build();

    log.info(
        "Sending transactions to sedex outbox. [jobId:{}; senderId:{}, page:{}; numTransactions:{}]",
        messageId,
        senderId,
        0,
        processedPersonDataList.size());

    final byte[] byteProcessedSedexPersonData =
        BinarySerializerUtil.convertObjectToByteArray(jobCollectedPersonData);

    final CommonHeadersDao headersDao =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.JOB_EVENT)
            .senderId(senderId)
            .jobState(JobState.NEW)
            .jobId(jobCollectedPersonData.getJobId())
            .jobType(JobType.PARTIAL)
            .timestamp(Instant.now())
            .build();

    final BasicProperties properties =
        (new BasicProperties.Builder())
            .headers(headersDao.toMap())
            .correlationId(jobCollectedPersonData.getJobId().toString())
            .build();

    channel.basicPublish(Exchanges.LWGS, outTopicName, properties, byteProcessedSedexPersonData);
  }

  public void processPartialQueue(
      @NonNull final String inQueueName, @NonNull final String outTopicName) {
    log.debug("Start processing queue " + inQueueName + ".");
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

          try {
            final Map<String, List<ProcessedPersonData>> senderIdMappedProcessedPersonData =
                processedPersonDataList.stream()
                    .collect(groupingBy(ProcessedPersonData::getSenderId));

            for (Map.Entry<String, List<ProcessedPersonData>> entry :
                senderIdMappedProcessedPersonData.entrySet()) {
              sendPartialMessage(channel, outTopicName, entry.getKey(), entry.getValue());
            }

          } catch (SerializationFailedException e) {
            log.warn(
                "Serialization of JobCollectedPersonData failed. Rolling back all transactions["
                    + getTransactionIds(processedPersonDataList).toString()
                    + "]");
            channel.txRollback();
          } finally {
            channel.txCommit();
          }
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

  private List<UUID> getTransactionIds(
      @NonNull final List<ProcessedPersonData> processedPersonDataList) {
    return processedPersonDataList.stream()
        .map(ProcessedPersonData::getTransactionId)
        .collect(Collectors.toList());
  }

  private List<ProcessedPersonData> getMessagesUntilPageFullOrQueueIsEmpty(
      @NonNull final Channel channel, @NonNull final String inQueueName) throws IOException {
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
      @NonNull final ProcessedPersonData processedPersonData,
      @NonNull final String senderId) {
    out(
        jobType,
        outTopicName,
        JobCollectedPersonData.builder()
            .senderId(senderId)
            .jobId(UUID.randomUUID())
            .messageId(UUID.randomUUID())
            .processedPersonDataList(Collections.singletonList(processedPersonData))
            .build(),
        senderId);
  }

  private void out(
      @NonNull final JobType jobType,
      @NonNull final String topicName,
      @NonNull final JobCollectedPersonData jobCollectedPersonData,
      @NonNull final String senderId) {

    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
            .senderId(senderId)
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
