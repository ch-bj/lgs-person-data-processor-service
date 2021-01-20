package org.datarocks.lwgs.searchindex.client.service.state;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.BusinessValidationLog;
import org.datarocks.lwgs.searchindex.client.entity.SyncJob;
import org.datarocks.lwgs.searchindex.client.entity.Transaction;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.entity.type.TransactionState;
import org.datarocks.lwgs.searchindex.client.model.JobCollectedPersonData;
import org.datarocks.lwgs.searchindex.client.repository.BusinessLogRepository;
import org.datarocks.lwgs.searchindex.client.repository.SyncJobRepository;
import org.datarocks.lwgs.searchindex.client.repository.TransactionRepository;
import org.datarocks.lwgs.searchindex.client.service.amqp.CommonHeadersDao;
import org.datarocks.lwgs.searchindex.client.service.amqp.MessageCategory;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.util.BinarySerializerUtil;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StateProcessor {
  private final SyncJobRepository syncJobRepository;
  private final TransactionRepository transactionRepository;
  private final BusinessLogRepository businessLogRepository;
  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public StateProcessor(
      SyncJobRepository syncJobRepository,
      TransactionRepository transactionRepository,
      BusinessLogRepository businessLogRepository,
      RabbitTemplate rabbitTemplate) {
    this.syncJobRepository = syncJobRepository;
    this.transactionRepository = transactionRepository;
    this.businessLogRepository = businessLogRepository;
    this.rabbitTemplate = rabbitTemplate;
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
                      transaction.setSyncJob(job);
                      transaction.setUpdatedAt(headers.getTimestamp());
                      transaction.setState(TransactionState.JOB_ASSOCIATED);
                      transactionRepository.save(transaction);
                    });
              });
    }
  }

  private void handleJobMessage(CommonHeadersDao headers, Message message) {
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

  private Optional<SyncJob> createOrFetchSyncJob(CommonHeadersDao headers) {
    Optional<SyncJob> optionalSyncJob = syncJobRepository.findByJobId(headers.getJobId());

    if (optionalSyncJob.isPresent()) {
      return optionalSyncJob;
    }

    return Optional.of(
        syncJobRepository.save(
            SyncJob.builder()
                .jobId(headers.getJobId())
                .jobState(JobState.NEW)
                .jobType(headers.getJobType())
                .createdAt(headers.getTimestamp())
                .build()));
  }

  private Function<SyncJob, SyncJob> visit(Consumer<SyncJob> consumer) {
    return syncJob -> {
      consumer.accept(syncJob);
      return syncJob;
    };
  }

  private void processNewTransactionMessage(CommonHeadersDao headers) {
    Transaction transaction =
        Transaction.builder()
            .transactionId(headers.getTransactionId())
            .state(TransactionState.NEW)
            .createdAt(headers.getTimestamp())
            .updatedAt(headers.getTimestamp())
            .build();

    Optional<UUID> optionalJobId = headers.getOptionalJobId();

    if (optionalJobId.isPresent()) {
      createOrFetchSyncJob(headers)
          .map(visit(job -> job.setNumPersonMutations(job.getNumPersonMutations() + 1)))
          .map(visit(transaction::setSyncJob))
          .ifPresent(syncJobRepository::save);
    }
    transactionRepository.save(transaction);
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
  private void handleTransactionMessage(CommonHeadersDao headers) {
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

  private void handleBusinessLogMessage(Message message) {
    try {
      final BusinessValidationLog log =
          (BusinessValidationLog) rabbitTemplate.getMessageConverter().fromMessage(message);
      businessLogRepository.save(log);
    } catch (ClassCastException exception) {
      log.error("Dropping wrongly encoded business log message.");
    }
  }

  @RabbitListener(queues = Queues.JOB_STATE)
  private void listen(Message message) {
    log.info("Process message: " + message.toString());
    CommonHeadersDao headers = new CommonHeadersDao(message.getMessageProperties().getHeaders());
    switch (headers.getOptionalMessageCategory().orElse(MessageCategory.UNKNOWN)) {
      case TRANSACTION_EVENT:
        handleTransactionMessage(headers);
        break;
      case JOB_EVENT:
        handleJobMessage(headers, message);
        break;
      case BUSINESS_VALIDATION_LOG:
        handleBusinessLogMessage(message);
        break;
      default:
        log.warn("Unknown message received for state processing: " + message.toString());
    }
  }
}
