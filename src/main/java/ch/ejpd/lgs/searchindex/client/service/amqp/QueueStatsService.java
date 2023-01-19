package ch.ejpd.lgs.searchindex.client.service.amqp;

import java.util.Optional;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QueueStatsService {
  private final RabbitAdmin rabbitAdmin;

  @Autowired
  public QueueStatsService(RabbitAdmin rabbitAdmin) {
    this.rabbitAdmin = rabbitAdmin;
  }

  public int getQueueCount(String queueName) {
    return Optional.ofNullable(rabbitAdmin.getQueueInfo(queueName))
        .map(QueueInformation::getMessageCount)
        .orElse(0);
  }
}
