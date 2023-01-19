package ch.ejpd.lgs.searchindex.client.service.processor;

import ch.ejpd.lgs.persondataprocessor.processor.gbpersonprocessor.event.DuplicatedAttributeDroppedProcessorEvent;
import ch.ejpd.lgs.persondataprocessor.processor.gbpersonprocessor.event.InvalidAttributeDroppedProcessorEvent;
import ch.ejpd.lgs.persondataprocessor.processor.gbpersonprocessor.event.RequiredAttributesMissingProcessorEvent;
import ch.ejpd.lgs.searchindex.client.entity.BusinessValidationLog;
import ch.ejpd.lgs.searchindex.client.entity.type.BusinessValidationEventType;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.Exchanges;
import ch.ejpd.lgs.searchindex.client.service.amqp.MessageCategory;
import ch.ejpd.lgs.searchindex.client.service.amqp.Topics;
import ch.ejpd.lgs.searchindex.client.service.exception.BusinessValidationException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.banzai.event.ProcessorEvent;
import org.datarocks.banzai.event.ProcessorEventListener;
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
