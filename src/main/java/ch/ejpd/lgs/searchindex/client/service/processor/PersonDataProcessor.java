package ch.ejpd.lgs.searchindex.client.service.processor;

import ch.ejpd.lgs.persondataprocessor.model.GBPersonEvent;
import ch.ejpd.lgs.persondataprocessor.transformer.exception.InvalidJsonStructure;
import ch.ejpd.lgs.searchindex.client.entity.type.TransactionState;
import ch.ejpd.lgs.searchindex.client.model.PersonData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonDataFailed;
import ch.ejpd.lgs.searchindex.client.service.amqp.*;
import ch.ejpd.lgs.searchindex.client.service.exception.BusinessValidationException;
import ch.ejpd.lgs.searchindex.client.service.exception.ProcessingPersonDataFailedException;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.banzai.exception.RequiredParameterMissing;
import org.datarocks.banzai.pipeline.PipeLine;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PersonDataProcessor {

  private final RabbitTemplate rabbitTemplate;
  private final PipeLine<String, GBPersonEvent, String> pipeLine;
  private static final String EMPTY_PAYLOAD = "";

  @Autowired
  public PersonDataProcessor(
      RabbitTemplate rabbitTemplate, PipeLine<String, GBPersonEvent, String> pipeLine) {
    this.rabbitTemplate = rabbitTemplate;
    this.pipeLine = pipeLine;
  }

  @RabbitListener(queues = Queues.PERSONDATA_PARTIAL_INCOMING, concurrency = "1-4")
  public void listenPartial(PersonData personData, @Headers Map<String, Object> rawHeaders) {
    final CommonHeadersDao headers = new CommonHeadersDao(rawHeaders);
    try {
      out(
          Topics.PERSONDATA_PARTIAL_OUTGOING,
          process(personData, headers.getSenderId(), headers.getLandRegister()),
          headers.getSenderId());
    } catch (ProcessingPersonDataFailedException e) {
      log.warn("Failed processing transaction: {}", e.getMessage());
      outFailed(
          Topics.PERSONDATA_PARTIAL_FAILED,
          ProcessedPersonDataFailed.builder()
              .senderId(headers.getSenderId())
              .landRegister(headers.getLandRegister())
              .transactionId(personData.getTransactionId())
              .failureReason(e.getMessage())
              .payload(personData.getPayload())
              .build(),
          headers.getSenderId());
    }
  }

  @RabbitListener(queues = Queues.PERSONDATA_FULL_INCOMING, concurrency = "1-4")
  public void listenFull(PersonData personData, @Headers Map<String, Object> rawHeaders) {
    final CommonHeadersDao headers = new CommonHeadersDao(rawHeaders);
    try {
      out(
          Topics.PERSONDATA_FULL_OUTGOING,
          process(personData, headers.getSenderId(), headers.getLandRegister()),
          headers.getSenderId());
    } catch (ProcessingPersonDataFailedException e) {
      log.warn("Failed processing transaction: {}", e.getMessage());
      outFailed(
          Topics.PERSONDATA_FULL_FAILED,
          ProcessedPersonDataFailed.builder()
              .senderId(headers.getSenderId())
              .landRegister(headers.getLandRegister())
              .transactionId(personData.getTransactionId())
              .failureReason(e.getMessage())
              .payload(personData.getPayload())
              .build(),
          headers.getSenderId());
    }
  }

  private ProcessedPersonData process(
      @NonNull final PersonData personData,
      @NonNull final String senderId,
      final String landRegister)
      throws ProcessingPersonDataFailedException {
    try {
      String processingResult =
          pipeLine.process(personData.getTransactionId().toString(), personData.getPayload());
      return ProcessedPersonData.builder()
          .senderId(senderId)
          .landRegister(landRegister)
          .transactionId(personData.getTransactionId())
          .payload(processingResult)
          .build();
    } catch (RequiredParameterMissing e) {
      log.error(
          "Stop processing message "
              + personData.getTransactionId()
              + ". Required pipeline configuration parameter missing.");
      throw new ListenerExecutionFailedException(
          "Required pipeline configuration parameter missing.", e);
    } catch (InvalidJsonStructure e) {
      log.warn(
          "Invalid Json structure of gb person event. TransactionId: "
              + personData.getTransactionId()
              + ". Sending to failed queue.");
      throw new ProcessingPersonDataFailedException(e);
    } catch (BusinessValidationException e) {
      log.warn(
          "Business validation of gb person event failed. TransactionId: "
              + personData.getTransactionId()
              + ". Sending gb person event to failed queue."
              + e);
      throw new ProcessingPersonDataFailedException(e);
    } catch (RuntimeException e) {
      log.error(
          "Fatal error in person data processor library. TransactionId: "
              + personData.getTransactionId()
              + ". Sending gb person event to failed queue."
              + e);
      throw new ProcessingPersonDataFailedException(e);
    }
  }

  private void out(
      @NonNull final String topicName,
      @NonNull final ProcessedPersonData processedPersonData,
      @NonNull final String senderId) {
    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
            .senderId(senderId)
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionId(processedPersonData.getTransactionId())
            .transactionState(TransactionState.PROCESSED)
            .timestamp()
            .build();

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS,
        topicName,
        processedPersonData,
        headers::applyAndSetTransactionIdAsCorrelationId);

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS_STATE,
        topicName,
        EMPTY_PAYLOAD,
        headers::applyAndSetTransactionIdAsCorrelationId);
  }

  private void outFailed(
      @NonNull final String topicName,
      @NonNull final ProcessedPersonDataFailed processedPeronData,
      @NonNull final String senderId) {
    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
            .senderId(senderId)
            .landRegister(processedPeronData.getLandRegister())
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionId(processedPeronData.getTransactionId())
            .transactionState(TransactionState.FAILED)
            .timestamp()
            .build();

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS,
        topicName,
        processedPeronData,
        headers::applyAndSetTransactionIdAsCorrelationId);

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS_STATE,
        topicName,
        EMPTY_PAYLOAD,
        headers::applyAndSetTransactionIdAsCorrelationId);
  }
}
