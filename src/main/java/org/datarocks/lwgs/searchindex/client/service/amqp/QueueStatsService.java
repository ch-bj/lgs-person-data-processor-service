package org.datarocks.lwgs.searchindex.client.service.amqp;

import java.util.Properties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueueStatsService {
  private static final String MESSAGE_COUNT_PROPERTY = "QUEUE_MESSAGE_COUNT";
  private final RabbitAdmin rabbitAdmin;

  @Autowired
  public QueueStatsService(RabbitAdmin rabbitAdmin) {
    this.rabbitAdmin = rabbitAdmin;
  }

  public int getQueueCount(String queueName) {
    Properties props = this.rabbitAdmin.getQueueProperties(queueName);
    if (props == null) { // NOSONAR: sonar mistakenly assumes getQueueProperties never returns null
      return 0;
    }
    return (Integer) props.get(MESSAGE_COUNT_PROPERTY);
  }
}
