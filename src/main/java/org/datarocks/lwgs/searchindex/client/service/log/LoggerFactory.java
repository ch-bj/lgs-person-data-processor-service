package org.datarocks.lwgs.searchindex.client.service.log;

import java.util.Date;
import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.Log;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;
import org.datarocks.lwgs.searchindex.client.service.amqp.Exchanges;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoggerFactory implements RawLogger {
  private static final String TOPIC_PREFIX = "lwgs.log.";
  private final RabbitTemplate rabbitTemplate;

  @Autowired
  public LoggerFactory(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public Logger getLogger(SourceType sourceType) {
    return new SimpleLogger(sourceType, this);
  }

  @Override
  public void log(
      Date timestamp,
      SourceType source,
      SeverityType severity,
      String message,
      UUID correlationId) {

    final Log log =
        Log.builder()
            .timestamp(timestamp)
            .source(source)
            .severity(severity)
            .message(message)
            .build();

    if (correlationId != null) {
      switch (source) {
        case PERSON_DATA_PROCESSOR:
          log.setTransactionId(correlationId);
        case PARTIAL_SYNC_PROCESSOR:
        case FULL_SYNC_PROCESSOR:
        case SEDEX_HANDLER:
          log.setJobId(correlationId);
          break;
      }
    }

    this.rabbitTemplate.convertAndSend(Exchanges.LOG, TOPIC_PREFIX + source.toString(), log);
  }
}
