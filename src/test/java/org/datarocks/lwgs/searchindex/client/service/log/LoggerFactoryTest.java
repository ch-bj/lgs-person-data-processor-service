package org.datarocks.lwgs.searchindex.client.service.log;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.Log;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;
import org.datarocks.lwgs.searchindex.client.service.amqp.Exchanges;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class LoggerFactoryTest {

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final LoggerFactory loggerFactory = new LoggerFactory(rabbitTemplate);

  @Test
  void getLogger() {
    final Logger logger = loggerFactory.getLogger(SourceType.PERSON_DATA_PROCESSOR);

    Assertions.assertNotNull(logger);
  }

  @Test
  void log() {
    final SourceType source = SourceType.PERSON_DATA_PROCESSOR;
    final String message = "Test message";
    final Logger logger = loggerFactory.getLogger(source);

    logger.error("Error");
    logger.debug(message);

    ArgumentCaptor<String> exchangeArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Log> logArgumentCaptor = ArgumentCaptor.forClass(Log.class);

    verify(rabbitTemplate, times(2))
        .convertAndSend(exchangeArgumentCaptor.capture(), anyString(), logArgumentCaptor.capture());

    assertAll(
        () -> assertEquals(Exchanges.LOG, exchangeArgumentCaptor.getValue()),
        () -> assertEquals(message, logArgumentCaptor.getValue().getMessage()),
        () -> assertEquals(SeverityType.DEBUG, logArgumentCaptor.getValue().getSeverity()),
        () ->
            assertEquals(
                SourceType.PERSON_DATA_PROCESSOR, logArgumentCaptor.getValue().getSource()));
  }

  @Test
  void logWithTransactionIdAsCorrelationId() {
    final SourceType source = SourceType.PERSON_DATA_PROCESSOR;
    final String message = "Test message";
    final Logger logger = loggerFactory.getLogger(source);
    final UUID transactionId = UUID.randomUUID();

    logger.info(message, transactionId);

    ArgumentCaptor<Log> logArgumentCaptor = ArgumentCaptor.forClass(Log.class);

    verify(rabbitTemplate, times(1))
        .convertAndSend(anyString(), anyString(), logArgumentCaptor.capture());

    assertEquals(transactionId, logArgumentCaptor.getValue().getTransactionId());
    assertNull(logArgumentCaptor.getValue().getJobId());
  }

  @Test
  void logWithJobIdAsCorrelationId() {
    final SourceType source = SourceType.FULL_SYNC_PROCESSOR;
    final String message = "Test message";
    final Logger logger = loggerFactory.getLogger(source);
    final UUID jobId = UUID.randomUUID();

    logger.warn(message, jobId);

    ArgumentCaptor<Log> logArgumentCaptor = ArgumentCaptor.forClass(Log.class);

    verify(rabbitTemplate, times(1))
        .convertAndSend(anyString(), anyString(), logArgumentCaptor.capture());

    assertEquals(jobId, logArgumentCaptor.getValue().getJobId());
    assertNull(logArgumentCaptor.getValue().getTransactionId());
  }
}
