package ch.ejpd.lgs.searchindex.client.service.amqp;

import java.util.Optional;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for retrieving statistics about AMQP queues.
 */
@Service
public class QueueStatsService {
  private final RabbitAdmin rabbitAdmin;

  @Autowired
  public QueueStatsService(RabbitAdmin rabbitAdmin) {
    this.rabbitAdmin = rabbitAdmin;
  }

  /**
   * Get the count of messages in the specified queue.
   *
   * @param queueName The name of the queue.
   * @return The count of messages in the queue.
   */
  public int getQueueCount(String queueName) {
    return Optional.ofNullable(rabbitAdmin.getQueueInfo(queueName))
        .map(QueueInformation::getMessageCount)
        .orElse(0);
  }
}
