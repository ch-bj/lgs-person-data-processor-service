package ch.ejpd.lgs.searchindex.client.service.state;

import ch.ejpd.lgs.searchindex.client.entity.SyncJob;
import ch.ejpd.lgs.searchindex.client.entity.Transaction;
import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.TransactionState;
import ch.ejpd.lgs.searchindex.client.model.JobCollectedPersonData;
import ch.ejpd.lgs.searchindex.client.repository.SyncJobRepository;
import ch.ejpd.lgs.searchindex.client.repository.TransactionRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.MessageCategory;
import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.util.BinarySerializerUtil;
import java.net.http.HttpClient;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
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
  public void handleJobMessage(final CommonHeadersDao headers, final Message message) {
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
