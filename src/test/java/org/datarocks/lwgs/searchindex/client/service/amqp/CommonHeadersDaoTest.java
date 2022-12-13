package org.datarocks.lwgs.searchindex.client.service.amqp;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.entity.type.TransactionState;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class CommonHeadersDaoTest {

  @Test
  void apply() {
    final Message dummyMessage = new Message("".getBytes(), new MessageProperties());
    final CommonHeadersDao dao =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.BUSINESS_VALIDATION_LOG)
            .timestamp()
            .build();

    dao.apply(dummyMessage);

    assertAll(
        () ->
            assertEquals(
                MessageCategory.BUSINESS_VALIDATION_LOG.toString(),
                dummyMessage.getMessageProperties().getHeader(Headers.MESSAGE_CATEGORY)),
        () -> assertNotNull(dummyMessage.getMessageProperties().getHeader(Headers.TIMESTAMP)));
  }

  @Test
  void applyAndSetJobIdAsCorrelationId() {
    final Message dummyMessage = new Message("".getBytes(), new MessageProperties());
    final UUID jobId = UUID.randomUUID();
    final CommonHeadersDao dao =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.JOB_EVENT)
            .timestamp()
            .jobId(jobId)
            .jobType(JobType.FULL)
            .jobState(JobState.NEW)
            .build();

    dao.applyAndSetJobIdAsCorrelationId(dummyMessage);

    assertAll(
        () ->
            assertEquals(jobId.toString(), dummyMessage.getMessageProperties().getCorrelationId()),
        () ->
            assertEquals(
                MessageCategory.JOB_EVENT.toString(),
                dummyMessage.getMessageProperties().getHeader(Headers.MESSAGE_CATEGORY)),
        () ->
            assertEquals(
                jobId.toString(), dummyMessage.getMessageProperties().getHeader(Headers.JOB_ID)),
        () ->
            assertEquals(
                JobState.NEW.toString(),
                dummyMessage.getMessageProperties().getHeader(Headers.JOB_STATE)),
        () ->
            assertEquals(
                JobType.FULL.toString(),
                dummyMessage.getMessageProperties().getHeader(Headers.JOB_TYPE)),
        () -> assertNotNull(dummyMessage.getMessageProperties().getHeader(Headers.TIMESTAMP)));
  }

  @Test
  void applyAndSetTransactionIdAsCorrelationId() {
    final UUID transactionId = UUID.randomUUID();
    final Message dummyMessage = new Message("".getBytes(), new MessageProperties());
    final CommonHeadersDao dao =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .timestamp()
            .transactionId(transactionId)
            .transactionState(TransactionState.NEW)
            .build();

    dao.applyAndSetTransactionIdAsCorrelationId(dummyMessage);

    assertAll(
        () ->
            assertEquals(
                transactionId.toString(), dummyMessage.getMessageProperties().getCorrelationId()),
        () ->
            assertEquals(
                MessageCategory.TRANSACTION_EVENT.toString(),
                dummyMessage.getMessageProperties().getHeader(Headers.MESSAGE_CATEGORY)),
        () ->
            assertEquals(
                transactionId.toString(),
                dummyMessage.getMessageProperties().getHeader(Headers.TRANSACTION_ID)),
        () ->
            assertEquals(
                TransactionState.NEW.toString(),
                dummyMessage.getMessageProperties().getHeader(Headers.TRANSACTION_STATE)),
        () -> assertNotNull(dummyMessage.getMessageProperties().getHeader(Headers.TIMESTAMP)));
  }

  @Test
  void toMap() {
    final UUID transactionId = UUID.randomUUID();
    final CommonHeadersDao dao =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .timestamp()
            .transactionId(transactionId)
            .transactionState(TransactionState.NEW)
            .build();

    final Map<String, Object> map = dao.toMap();

    assertAll(
        () -> assertTrue(map.containsKey(Headers.TRANSACTION_ID)),
        () -> assertTrue(map.containsKey(Headers.TRANSACTION_STATE)),
        () -> assertTrue(map.containsKey(Headers.TIMESTAMP)),
        () -> assertEquals(transactionId.toString(), map.get(Headers.TRANSACTION_ID)));
  }

  @Test
  void testBuilder() {
    final UUID jobId = UUID.randomUUID();
    final UUID transactionId = UUID.randomUUID();
    final CommonHeadersDao dao =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .timestamp()
            .jobId(jobId)
            .jobType(JobType.FULL)
            .jobState(JobState.NEW)
            .transactionId(transactionId)
            .transactionState(TransactionState.JOB_ASSOCIATED)
            .build();

    assertAll(
        () -> assertNotNull(dao.getTimestamp()),
        () -> assertEquals(MessageCategory.TRANSACTION_EVENT, dao.getMessageCategory()),
        () -> assertEquals(jobId, dao.getJobId()),
        () -> assertEquals(JobType.FULL, dao.getJobType()),
        () -> assertEquals(JobState.NEW, dao.getJobState()),
        () -> assertEquals(transactionId, dao.getTransactionId()),
        () -> assertEquals(TransactionState.JOB_ASSOCIATED, dao.getTransactionState()));
  }

  @Test
  void testBuilderTimestampDefault() {
    final CommonHeadersDao dao = CommonHeadersDao.builder().timestamp().build();
    assertNotNull(dao.getTimestamp());
  }

  @Test
  void testBuilderTimestamp() {
    final CommonHeadersDao dao =
        CommonHeadersDao.builder().timestamp(Instant.ofEpochSecond(0)).build();
    assertEquals(0, dao.getTimestamp().getTime());
  }

  @Test
  void testCopyBuilder() {
    final UUID jobId = UUID.randomUUID();
    final CommonHeadersDao dao0 =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.JOB_EVENT)
            .timestamp()
            .jobId(jobId)
            .jobType(JobType.FULL)
            .jobState(JobState.NEW)
            .build();

    final CommonHeadersDao dao1 =
        CommonHeadersDao.builder(dao0).timestamp().jobState(JobState.COMPLETED).build();

    assertAll(
        () -> assertTrue(dao0.getTimestamp().getTime() < dao1.getTimestamp().getTime()),
        () -> assertEquals(dao0.getMessageCategory(), dao1.getMessageCategory()),
        () -> assertEquals(jobId, dao1.getJobId()),
        () -> assertEquals(JobType.FULL, dao1.getJobType()),
        () -> assertEquals(JobState.COMPLETED, dao1.getJobState()),
        () -> assertFalse(dao1.getOptionalTransactionId().isPresent()),
        () -> assertFalse(dao1.getOptionalTransactionState().isPresent()));
  }
}
