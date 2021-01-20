package org.datarocks.lwgs.searchindex.client.service.log;

import org.datarocks.lwgs.searchindex.client.entity.Log;
import org.datarocks.lwgs.searchindex.client.repository.LogRepository;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseLogWriterService {
  private final LogRepository logRepository;

  @Autowired
  public DatabaseLogWriterService(LogRepository logRepository) {
    this.logRepository = logRepository;
  }

  @Transactional
  @RabbitListener(queues = Queues.LOGS)
  void listen(Log logMessage) {
    logRepository.save(logMessage);
  }
}
