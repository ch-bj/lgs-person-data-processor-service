package org.datarocks.lwgs.searchindex.client.service.processor;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.persondataprocessor.common.event.ProcessorEvent;
import org.datarocks.lwgs.persondataprocessor.common.event.ProcessorEventListener;
import org.datarocks.lwgs.persondataprocessor.processor.gbpersonprocessor.event.DuplicatedAttributeDroppedProcessorEvent;
import org.datarocks.lwgs.persondataprocessor.processor.gbpersonprocessor.event.InvalidAttributeDroppedProcessorEvent;
import org.datarocks.lwgs.persondataprocessor.processor.gbpersonprocessor.event.RequiredAttributesMissingProcessorEvent;
import org.datarocks.lwgs.searchindex.client.entity.BusinessValidationLog;
import org.datarocks.lwgs.searchindex.client.entity.type.BusinessValidationEventType;
import org.datarocks.lwgs.searchindex.client.service.amqp.*;
import org.datarocks.lwgs.searchindex.client.service.exception.BusinessValidationException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BusinessValidationEventProcessor implements ProcessorEventListener {
  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public BusinessValidationEventProcessor(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void processorEvent(ProcessorEvent processorEvent) {
    log.warn("Received business validation event: " + processorEvent.toString());

    BusinessValidationEventType businessValidationEventType;
    if (processorEvent instanceof DuplicatedAttributeDroppedProcessorEvent) {
      businessValidationEventType = BusinessValidationEventType.DUPLICATED_ATTRIBUTE_DROPPED;
    } else if (processorEvent instanceof InvalidAttributeDroppedProcessorEvent) {
      businessValidationEventType = BusinessValidationEventType.INVALID_ATTRIBUTES_DROPPED;
    } else if (processorEvent instanceof RequiredAttributesMissingProcessorEvent) {
      businessValidationEventType = BusinessValidationEventType.REQUIRED_ATTRIBUTES_MISSING;
    } else {
      businessValidationEventType = BusinessValidationEventType.UNKNOWN;
    }

    final BusinessValidationLog log =
        BusinessValidationLog.builder()
            .timestamp(Date.from(Instant.now()))
            .transactionId(UUID.fromString(processorEvent.getCorrelationId()))
            .event(businessValidationEventType)
            .message(processorEvent.getMessage())
            .build();

    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
            .messageCategory(MessageCategory.BUSINESS_VALIDATION_LOG)
            .timestamp()
            .build();

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS, Topics.PERSONDATA_BUSINESS_VALIDATION, log, headers::apply);

    if (processorEvent instanceof RequiredAttributesMissingProcessorEvent) {
      throw new BusinessValidationException(
          "Processing of gb person data failed: " + processorEvent.getMessage());
    }
  }
}
