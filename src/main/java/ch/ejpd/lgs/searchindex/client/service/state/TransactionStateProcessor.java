package ch.ejpd.lgs.searchindex.client.service.state;

import ch.ejpd.lgs.searchindex.client.entity.SyncJob;
import ch.ejpd.lgs.searchindex.client.entity.Transaction;
import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.entity.type.TransactionState;
import ch.ejpd.lgs.searchindex.client.repository.SyncJobRepository;
import ch.ejpd.lgs.searchindex.client.repository.TransactionRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.MessageCategory;
import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TransactionStateProcessor {
  private final SyncJobRepository syncJobRepository;
  private final TransactionRepository transactionRepository;

  private final Map<UUID, SyncJob> syncJobCache = new HashMap<>();

  @Autowired
  public TransactionStateProcessor(
      SyncJobRepository syncJobRepository, TransactionRepository transactionRepository) {
    this.syncJobRepository = syncJobRepository;
    this.transactionRepository = transactionRepository;
  }

  @Synchronized
  private void createJob(CommonHeadersDao headers) {
    syncJobRepository.save(
        SyncJob.builder()
            .jobId(headers.getJobId())
            .jobState(JobState.NEW)
            .jobType(headers.getJobType())
            .createdAt(headers.getTimestamp())
            .build());
  }

  private void createJobIfNotExisting(CommonHeadersDao headers) {
    final UUID jobId = headers.getJobId();
    Optional<SyncJob> optionalSyncJob = Optional.ofNullable(syncJobCache.get(jobId));

    if (optionalSyncJob.isEmpty()) {
      optionalSyncJob = syncJobRepository.findByJobId(headers.getJobId());
      optionalSyncJob.ifPresent(job -> syncJobCache.put(job.getJobId(), job));
    }

    if (optionalSyncJob.isPresent()) {
      return;
    }

    createJob(headers);
  }

  private void processNewTransactionMessage(CommonHeadersDao headers) {
    Transaction transaction =
        Transaction.builder()
            .transactionId(headers.getTransactionId())
            .state(TransactionState.NEW)
            .createdAt(headers.getTimestamp())
            .updatedAt(headers.getTimestamp())
            .build();

    try {
      Optional<UUID> optionalJobId = headers.getOptionalJobId();

      if (optionalJobId.isPresent()) {
        createJobIfNotExisting(headers);
        transaction.setJobId(optionalJobId.get());
      }
      transactionRepository.save(transaction);
    } catch (DataIntegrityViolationException e) {
      log.debug("Transaction already existing");
    } catch (Exception e) {
      log.warn("Got exception saving transaction", e);
    }
  }

  private void updateJobStateIfRequired(CommonHeadersDao headers) {
    Optional<UUID> optionalJobId = headers.getOptionalJobId();

    // If we're in a full-sync and one of the transactions is failing, we want the job to reflect
    // this state.
    if (optionalJobId.isPresent()) {
      Optional<SyncJob> optionalSyncJob = syncJobRepository.findByJobId(optionalJobId.get());

      optionalSyncJob.ifPresent(
          job -> {
            if (job.getJobType() == JobType.FULL) {
              job.setStateWithTimestamp(JobState.FAILED_PROCESSING, headers.getTimestamp());
              syncJobRepository.save(job);
            }
          });
    }
  }

  @SuppressWarnings({"squid:S128"})
  @Transactional
  public void handleTransactionMessage(CommonHeadersDao headers) {
    switch (headers.getTransactionState()) {
      case NEW:
        processNewTransactionMessage(headers);
        break;
      case FAILED:
        updateJobStateIfRequired(headers);
      default:
        transactionRepository
            .findByTransactionId(headers.getTransactionId())
            .ifPresent(
                transaction -> {
                  transaction.setState(headers.getTransactionState());
                  transaction.setUpdatedAt(headers.getTimestamp());
                  transactionRepository.save(transaction);
                });
    }
  }

  @RabbitListener(queues = Queues.TRANSACTION_STATE, concurrency = "2-16", priority = "10")
  protected void listen(Message message) {
    CommonHeadersDao headers = new CommonHeadersDao(message.getMessageProperties().getHeaders());
    if (headers.getOptionalMessageCategory().orElse(MessageCategory.UNKNOWN)
        == MessageCategory.TRANSACTION_EVENT) {
      handleTransactionMessage(headers);
    }
  }
}
