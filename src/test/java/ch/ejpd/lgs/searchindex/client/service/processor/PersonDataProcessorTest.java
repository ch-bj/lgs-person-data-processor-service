package ch.ejpd.lgs.searchindex.client.service.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.ejpd.lgs.persondataprocessor.model.GBPersonEvent;
import ch.ejpd.lgs.searchindex.client.model.PersonData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonDataFailed;
import ch.ejpd.lgs.searchindex.client.service.amqp.Exchanges;
import ch.ejpd.lgs.searchindex.client.service.amqp.Headers;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.datarocks.banzai.pipeline.PipeLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class PersonDataProcessorTest {
  public static final String LAND_REGISTER = "Frauenfeld";
  public static final String SENDER_ID = "LGS-0032-CH";
  public static final String FULL_JOB_TYPE = "FULL";
  public static final String UUID_AS_STRING = "8e0de39d-a766-458b-a754-e2227955cdcb";
  public static final UUID TRANSACTION_ID = UUID.fromString(UUID_AS_STRING);
  private static final String payload =
      """
            {
            	"metaData": {
            		"personType":"NATUERLICHE_PERSON",
            		"eventType":"INSERT"
            	},
            	"natuerlichePerson": {
            		"egpId":"127",
            		"name":"Smithy",
            		"vorname":"Doe",
            		"jahrgang":"1970"
                }
            }
            """;

  private RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private PipeLine<String, GBPersonEvent, String> pipeLine = mock(PipeLine.class);

  private PersonDataProcessor personDataProcessor;

  @BeforeEach
  void setUp() {
    personDataProcessor = new PersonDataProcessor(rabbitTemplate, pipeLine);
  }

  @Test
  void listenFull_givenHappyPath() {
    Map<String, Object> rawHeaders = new HashMap<>();
    rawHeaders.put(Headers.SENDER_ID, SENDER_ID);
    rawHeaders.put(Headers.LAND_REGISTER, LAND_REGISTER);
    rawHeaders.put(Headers.JOB_TYPE, FULL_JOB_TYPE);

    PersonData personData =
        PersonData.builder().transactionId(TRANSACTION_ID).payload(payload).build();

    when(pipeLine.process(eq(UUID_AS_STRING), anyString())).thenReturn(payload);

    ProcessedPersonData processedPersonData =
        ProcessedPersonData.builder()
            .senderId(SENDER_ID)
            .landRegister(LAND_REGISTER)
            .transactionId(TRANSACTION_ID)
            .payload(payload)
            .build();

    personDataProcessor.listenFull(personData, rawHeaders);

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS),
            eq("topics.persondata.full.outgoing"),
            eq(processedPersonData),
            any(MessagePostProcessor.class));

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS_STATE),
            eq("topics.persondata.full.outgoing"),
            anyString(),
            any(MessagePostProcessor.class));
  }

  @Test
  void listenFull_givenExceptionIsThrown() {
    Map<String, Object> rawHeaders = new HashMap<>();
    rawHeaders.put(Headers.SENDER_ID, SENDER_ID);
    rawHeaders.put(Headers.LAND_REGISTER, LAND_REGISTER);
    rawHeaders.put(Headers.JOB_TYPE, FULL_JOB_TYPE);

    PersonData personData =
        PersonData.builder().transactionId(TRANSACTION_ID).payload(payload).build();

    ProcessedPersonDataFailed processedPersonDataFailed =
        ProcessedPersonDataFailed.builder()
            .landRegister(LAND_REGISTER)
            .senderId(SENDER_ID)
            .failureReason("java.lang.RuntimeException: runtime exception message")
            .transactionId(TRANSACTION_ID)
            .payload(payload)
            .build();

    when(pipeLine.process(eq(UUID_AS_STRING), anyString()))
        .thenThrow(new RuntimeException("runtime exception message"));

    personDataProcessor.listenFull(personData, rawHeaders);

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS),
            eq("topics.persondata.full.failed"),
            eq(processedPersonDataFailed),
            any(MessagePostProcessor.class));

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS_STATE),
            eq("topics.persondata.full.failed"),
            anyString(),
            any(MessagePostProcessor.class));
  }

  @Test
  void listenPartial_givenHappyPath() {
    Map<String, Object> rawHeaders = new HashMap<>();
    rawHeaders.put(Headers.SENDER_ID, SENDER_ID);
    rawHeaders.put(Headers.LAND_REGISTER, LAND_REGISTER);
    rawHeaders.put(Headers.JOB_TYPE, FULL_JOB_TYPE);

    PersonData personData =
        PersonData.builder().transactionId(TRANSACTION_ID).payload(payload).build();

    when(pipeLine.process(eq(UUID_AS_STRING), anyString())).thenReturn(payload);

    ProcessedPersonData processedPersonData =
        ProcessedPersonData.builder()
            .senderId(SENDER_ID)
            .landRegister(LAND_REGISTER)
            .transactionId(TRANSACTION_ID)
            .payload(payload)
            .build();

    personDataProcessor.listenPartial(personData, rawHeaders);

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS),
            eq("topics.persondata.partial.outgoing"),
            eq(processedPersonData),
            any(MessagePostProcessor.class));

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS_STATE),
            eq("topics.persondata.partial.outgoing"),
            anyString(),
            any(MessagePostProcessor.class));
  }

  @Test
  void listenPartial_givenExceptionIsThrown() {
    Map<String, Object> rawHeaders = new HashMap<>();
    rawHeaders.put(Headers.SENDER_ID, SENDER_ID);
    rawHeaders.put(Headers.LAND_REGISTER, LAND_REGISTER);
    rawHeaders.put(Headers.JOB_TYPE, FULL_JOB_TYPE);

    PersonData personData =
        PersonData.builder().transactionId(TRANSACTION_ID).payload(payload).build();

    ProcessedPersonDataFailed processedPersonDataFailed =
        ProcessedPersonDataFailed.builder()
            .landRegister(LAND_REGISTER)
            .senderId(SENDER_ID)
            .failureReason("java.lang.RuntimeException: runtime exception message")
            .transactionId(TRANSACTION_ID)
            .payload(payload)
            .build();

    when(pipeLine.process(eq(UUID_AS_STRING), anyString()))
        .thenThrow(new RuntimeException("runtime exception message"));

    personDataProcessor.listenPartial(personData, rawHeaders);

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS),
            eq("topics.persondata.partial.failed"),
            eq(processedPersonDataFailed),
            any(MessagePostProcessor.class));

    verify(rabbitTemplate, times(1))
        .convertAndSend(
            eq(Exchanges.LWGS_STATE),
            eq("topics.persondata.partial.failed"),
            anyString(),
            any(MessagePostProcessor.class));
  }
}
