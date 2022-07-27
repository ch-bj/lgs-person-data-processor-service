package org.datarocks.lwgs.searchindex.client.service.processor;

import lombok.extern.slf4j.Slf4j;
import org.datarocks.banzai.exception.RequiredParameterMissing;
import org.datarocks.banzai.pipeline.PipeLine;
import org.datarocks.lwgs.persondataprocessor.model.GBPersonEvent;
import org.datarocks.lwgs.persondataprocessor.transformer.exception.InvalidJsonStructure;
import org.datarocks.lwgs.searchindex.client.entity.type.TransactionState;
import org.datarocks.lwgs.searchindex.client.model.PersonData;
import org.datarocks.lwgs.searchindex.client.model.ProcessedPersonData;
import org.datarocks.lwgs.searchindex.client.model.ProcessedPersonDataFailed;
import org.datarocks.lwgs.searchindex.client.service.amqp.*;
import org.datarocks.lwgs.searchindex.client.service.exception.BusinessValidationException;
import org.datarocks.lwgs.searchindex.client.service.exception.ProcessingPersonDataFailedException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Autowired;
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

  @RabbitListener(queues = Queues.PERSONDATA_PARTIAL_INCOMING, concurrency = "1-8")
  public void listenPartial(PersonData personData) {
    try {
      out(Topics.PERSONDATA_PARTIAL_OUTGOING, process(personData));
    } catch (ProcessingPersonDataFailedException e) {
      log.warn("Failed processing transaction: {}", e.getMessage());
      outFailed(
          Topics.PERSONDATA_PARTIAL_FAILED,
          ProcessedPersonDataFailed.builder()
              .transactionId(personData.getTransactionId())
              .failureReason(e.getMessage())
              .payload(personData.getPayload())
              .build());
    }
  }

  @RabbitListener(queues = Queues.PERSONDATA_FULL_INCOMING, concurrency = "1-4")
  public void listenFull(PersonData personData) {
    try {
      out(Topics.PERSONDATA_FULL_OUTGOING, process(personData));
    } catch (ProcessingPersonDataFailedException e) {
      log.warn("Failed processing transaction: {}", e.getMessage());
      outFailed(
          Topics.PERSONDATA_FULL_FAILED,
          ProcessedPersonDataFailed.builder()
              .transactionId(personData.getTransactionId())
              .failureReason(e.getMessage())
              .payload(personData.getPayload())
              .build());
    }
  }

  private ProcessedPersonData process(PersonData personData)
      throws ProcessingPersonDataFailedException {
    try {
      String processingResult =
          pipeLine.process(personData.getTransactionId().toString(), personData.getPayload());
      return ProcessedPersonData.builder()
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

  private void out(String topicName, ProcessedPersonData processedPeronData) {
    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionId(processedPeronData.getTransactionId())
            .transactionState(TransactionState.PROCESSED)
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

  private void outFailed(String topicName, ProcessedPersonDataFailed processedPeronData) {
    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
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
