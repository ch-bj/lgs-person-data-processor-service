package org.datarocks.lwgs.searchindex.client.service.state;

import java.net.http.HttpClient;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.SyncJob;
import org.datarocks.lwgs.searchindex.client.entity.Transaction;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.TransactionState;
import org.datarocks.lwgs.searchindex.client.model.JobCollectedPersonData;
import org.datarocks.lwgs.searchindex.client.repository.SyncJobRepository;
import org.datarocks.lwgs.searchindex.client.repository.TransactionRepository;
import org.datarocks.lwgs.searchindex.client.service.amqp.CommonHeadersDao;
import org.datarocks.lwgs.searchindex.client.service.amqp.MessageCategory;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.util.BinarySerializerUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class JobStateProcessor {
  private final SyncJobRepository syncJobRepository;
  private final TransactionRepository transactionRepository;

  @Autowired
  public JobStateProcessor(
      SyncJobRepository syncJobRepository, TransactionRepository transactionRepository) {
    this.syncJobRepository = syncJobRepository;
    this.transactionRepository = transactionRepository;
  }

  private void createNewSyncJobFromMessage(CommonHeadersDao headers, Message message) {
    JobCollectedPersonData personData = null;

    try {
      personData =
          BinarySerializerUtil.convertByteArrayToObject(
              message.getBody(), JobCollectedPersonData.class);
    } catch (Exception e) {
      log.error("Deserialization error [{}]. Dropping message.", e.getMessage());
    }

    if (personData != null) {
      final SyncJob job =
          SyncJob.builder()
              .createdAt(headers.getTimestamp())
              .jobId(personData.getJobId())
              .jobState(JobState.NEW)
              .jobType(headers.getJobType())
              .numPersonMutations(personData.getProcessedPersonDataList().size())
              .build();

      syncJobRepository.save(job);

      personData
          .getProcessedPersonDataList()
          .forEach(
              e -> {
                Optional<Transaction> optionalTransaction =
                    transactionRepository.findByTransactionId(e.getTransactionId());
                optionalTransaction.ifPresent(
                    transaction -> {
                      transaction.setJobId(job.getJobId());
                      transaction.setUpdatedAt(headers.getTimestamp());
                      transaction.setState(TransactionState.JOB_ASSOCIATED);
                      transactionRepository.save(transaction);
                    });
              });
    }
  }

  @Transactional
  protected void handleJobMessage(final CommonHeadersDao headers, final Message message) {
    if (headers.getJobState() == JobState.NEW) {
      createNewSyncJobFromMessage(headers, message);
      return;
    }
    Optional<SyncJob> optionalSyncJob = syncJobRepository.findByJobId(headers.getJobId());
    optionalSyncJob.ifPresent(
        job -> {
          job.setStateWithTimestamp(headers.getJobState(), headers.getTimestamp());
          syncJobRepository.save(job);
        });
  }

  @RabbitListener(queues = Queues.JOB_STATE)
  protected void listen(final Message message) {
    final CommonHeadersDao headers =
        new CommonHeadersDao(message.getMessageProperties().getHeaders());
    if (headers.getOptionalMessageCategory().orElse(MessageCategory.UNKNOWN)
        == MessageCategory.JOB_EVENT) {
      handleJobMessage(headers, message);
    }
      HttpClient.newBuilder().build();
  }
}
