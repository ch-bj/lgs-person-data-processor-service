package ch.ejpd.lgs.searchindex.client.service.state;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.ejpd.lgs.searchindex.client.entity.SyncJob;
import ch.ejpd.lgs.searchindex.client.entity.Transaction;
import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.entity.type.TransactionState;
import ch.ejpd.lgs.searchindex.client.model.JobCollectedPersonData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;
import ch.ejpd.lgs.searchindex.client.repository.SyncJobRepository;
import ch.ejpd.lgs.searchindex.client.repository.TransactionRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.MessageCategory;
import ch.ejpd.lgs.searchindex.client.service.exception.SerializationFailedException;
import ch.ejpd.lgs.searchindex.client.util.BinarySerializerUtil;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class StateProcessorsTest {
  private static final byte[] EMPTY_PAYLOAD = "{}".getBytes();
  private static final String SENDER_ID_A = "LGS-123-AAA";

  private final SyncJobRepository syncJobRepository = mock(SyncJobRepository.class);
  private final TransactionRepository transactionRepository = mock(TransactionRepository.class);

  private final JobStateProcessor jobStateProcessor =
      new JobStateProcessor(syncJobRepository, transactionRepository);

  private final TransactionStateProcessor transactionStateProcessor =
      new TransactionStateProcessor(syncJobRepository, transactionRepository);

  final ArgumentCaptor<Transaction> transactionArgumentCaptor =
      ArgumentCaptor.forClass(Transaction.class);
  final ArgumentCaptor<SyncJob> jobArgumentCaptor = ArgumentCaptor.forClass(SyncJob.class);

  @Test
  void testProcessNewPartialTransaction() {
    final UUID transactionId = UUID.randomUUID();
    final Message message = new Message(EMPTY_PAYLOAD, new MessageProperties());
    final CommonHeadersDao commonHeadersDao =
        CommonHeadersDao.builder()
            .timestamp()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionId(transactionId)
            .transactionState(TransactionState.NEW)
            .build();

    jobStateProcessor.listen(commonHeadersDao.apply(message));
    transactionStateProcessor.listen(commonHeadersDao.apply(message));

    verify(syncJobRepository, times(0)).save(any());
    verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

    assertEquals(transactionId, transactionArgumentCaptor.getValue().getTransactionId());
  }

  @Test
  @Disabled("Needs rewrite")
  void testProcessNewFullTransactionWhenJobEmpty() {
    final UUID transactionId = UUID.randomUUID();
    final UUID jobId = UUID.randomUUID();
    final Message message = new Message(EMPTY_PAYLOAD, new MessageProperties());
    final CommonHeadersDao commonHeadersDao =
        CommonHeadersDao.builder()
            .timestamp()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionId(transactionId)
            .transactionState(TransactionState.NEW)
            .jobType(JobType.FULL)
            .jobId(jobId)
            .build();

    doReturn(SyncJob.builder().jobId(jobId).jobState(JobState.NEW).jobType(JobType.FULL).build())
        .when(syncJobRepository)
        .save(any());

    transactionStateProcessor.listen(commonHeadersDao.apply(message));

    verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

    assertAll(
        () -> assertEquals(transactionId, transactionArgumentCaptor.getValue().getTransactionId()),
        () -> assertEquals(jobId, jobArgumentCaptor.getValue().getJobId()),
        () -> Assertions.assertEquals(JobType.FULL, jobArgumentCaptor.getValue().getJobType()),
        () -> assertEquals(1, jobArgumentCaptor.getValue().getNumPersonMutations()));
  }

  @Test
  @Disabled("Needs rewrite")
  void testProcessNewFullTransactionWhenJobExisting() {
    final UUID transactionId = UUID.randomUUID();
    final UUID jobId = UUID.randomUUID();
    final Message message = new Message(EMPTY_PAYLOAD, new MessageProperties());
    final CommonHeadersDao commonHeadersDao =
        CommonHeadersDao.builder()
            .timestamp()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionId(transactionId)
            .transactionState(TransactionState.NEW)
            .jobType(JobType.FULL)
            .jobId(jobId)
            .build();

    doReturn(
            Optional.of(
                SyncJob.builder()
                    .jobId(jobId)
                    .jobState(JobState.NEW)
                    .jobType(JobType.FULL)
                    .numPersonMutations(1)
                    .build()))
        .when(syncJobRepository)
        .findByJobId(jobId);

    transactionStateProcessor.listen(commonHeadersDao.apply(message));

    verify(syncJobRepository, times(0)).save(jobArgumentCaptor.capture());
    verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

    assertAll(
        () -> assertEquals(transactionId, transactionArgumentCaptor.getValue().getTransactionId()),
        () -> assertEquals(jobId, jobArgumentCaptor.getValue().getJobId()),
        () -> Assertions.assertEquals(JobType.FULL, jobArgumentCaptor.getValue().getJobType()),
        () -> assertEquals(2, jobArgumentCaptor.getValue().getNumPersonMutations()));
  }

  @Test
  void testProcessFailedTransaction() {
    final UUID jobId = UUID.randomUUID();
    final UUID transactionId = UUID.randomUUID();
    final Message message = new Message(EMPTY_PAYLOAD, new MessageProperties());
    final CommonHeadersDao commonHeadersDao =
        CommonHeadersDao.builder()
            .timestamp()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionId(transactionId)
            .transactionState(TransactionState.FAILED)
            .jobId(jobId)
            .jobType(JobType.FULL)
            .build();

    doReturn(
            Optional.of(
                SyncJob.builder()
                    .jobId(jobId)
                    .jobState(JobState.NEW)
                    .jobType(JobType.FULL)
                    .numPersonMutations(1)
                    .build()))
        .when(syncJobRepository)
        .findByJobId(jobId);

    doReturn(
            Optional.of(
                Transaction.builder()
                    .transactionId(transactionId)
                    .state(TransactionState.NEW)
                    .jobId(jobId)
                    .build()))
        .when(transactionRepository)
        .findByTransactionId(transactionId);

    transactionStateProcessor.listen(commonHeadersDao.apply(message));

    verify(syncJobRepository, times(1)).save(jobArgumentCaptor.capture());
    verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

    assertAll(
        () -> assertEquals(transactionId, transactionArgumentCaptor.getValue().getTransactionId()),
        () ->
            Assertions.assertEquals(
                TransactionState.FAILED, transactionArgumentCaptor.getValue().getState()),
        () ->
            Assertions.assertEquals(
                JobState.FAILED_PROCESSING, jobArgumentCaptor.getValue().getJobState()));
  }

  @Test
  void testProcessNewJob() throws SerializationFailedException {
    final UUID jobId = UUID.randomUUID();
    final UUID transactionId = UUID.randomUUID();
    final UUID messageId = UUID.randomUUID();
    final JobCollectedPersonData collectedPersonData =
        JobCollectedPersonData.builder()
            .senderId(SENDER_ID_A)
            .jobId(jobId)
            .messageId(messageId)
            .processedPersonDataList(
                Collections.singletonList(
                    ProcessedPersonData.builder()
                        .senderId(SENDER_ID_A)
                        .transactionId(transactionId)
                        .payload("{}")
                        .build()))
            .build();
    final Message message =
        new Message(
            BinarySerializerUtil.convertObjectToByteArray(collectedPersonData),
            new MessageProperties());
    final CommonHeadersDao commonHeadersDao =
        CommonHeadersDao.builder()
            .timestamp()
            .messageCategory(MessageCategory.JOB_EVENT)
            .jobId(jobId)
            .jobType(JobType.FULL)
            .jobState(JobState.NEW)
            .build();

    doReturn(
            Optional.of(
                Transaction.builder()
                    .transactionId(transactionId)
                    .state(TransactionState.NEW)
                    .jobId(jobId)
                    .build()))
        .when(transactionRepository)
        .findByTransactionId(transactionId);

    doReturn(Optional.empty()).when(syncJobRepository).findByJobId(jobId);

    jobStateProcessor.listen(commonHeadersDao.apply(message));

    verify(syncJobRepository, times(1)).save(jobArgumentCaptor.capture());
    verify(transactionRepository, times(1)).save(transactionArgumentCaptor.capture());

    assertAll(
        () -> assertEquals(jobId, jobArgumentCaptor.getValue().getJobId()),
        () -> Assertions.assertEquals(JobState.NEW, jobArgumentCaptor.getValue().getJobState()),
        () ->
            Assertions.assertEquals(
                TransactionState.JOB_ASSOCIATED, transactionArgumentCaptor.getValue().getState()));
  }

  @Test
  void testProcessJobUpdate() throws SerializationFailedException {
    final UUID jobId = UUID.randomUUID();
    final UUID messageId = UUID.randomUUID();
    final JobCollectedPersonData collectedPersonData =
        JobCollectedPersonData.builder()
            .senderId(SENDER_ID_A)
            .jobId(jobId)
            .messageId(messageId)
            .processedPersonDataList(
                Collections.singletonList(
                    ProcessedPersonData.builder()
                        .senderId(SENDER_ID_A)
                        .transactionId(UUID.randomUUID())
                        .payload("{}")
                        .build()))
            .build();
    final Message message =
        new Message(
            BinarySerializerUtil.convertObjectToByteArray(collectedPersonData),
            new MessageProperties());
    final CommonHeadersDao commonHeadersDao =
        CommonHeadersDao.builder()
            .timestamp()
            .messageCategory(MessageCategory.JOB_EVENT)
            .jobId(jobId)
            .jobType(JobType.FULL)
            .jobState(JobState.SENT)
            .build();

    doReturn(
            Optional.of(
                SyncJob.builder()
                    .jobId(jobId)
                    .jobState(JobState.NEW)
                    .jobType(JobType.FULL)
                    .numPersonMutations(1)
                    .build()))
        .when(syncJobRepository)
        .findByJobId(jobId);

    jobStateProcessor.listen(commonHeadersDao.apply(message));

    verify(syncJobRepository, times(1)).save(jobArgumentCaptor.capture());

    assertAll(
        () -> assertEquals(jobId, jobArgumentCaptor.getValue().getJobId()),
        () -> Assertions.assertEquals(JobState.SENT, jobArgumentCaptor.getValue().getJobState()));
  }
}
