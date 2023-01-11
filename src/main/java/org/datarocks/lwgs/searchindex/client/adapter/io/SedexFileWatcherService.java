package org.datarocks.lwgs.searchindex.client.adapter.io;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.commons.filewatcher.FileEvent;
import org.datarocks.lwgs.commons.filewatcher.exception.WatchDirNotAccessibleException;
import org.datarocks.lwgs.searchindex.client.configuration.SedexConfiguration;
import org.datarocks.lwgs.searchindex.client.service.amqp.Exchanges;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SedexFileWatcherService extends AbstractFileWatcherService {
  private final RabbitTemplate template;

  @Autowired
  public SedexFileWatcherService(
      SedexConfiguration sedexConfiguration,
      RabbitTemplate template,
      @Value("${lwgs.searchindex.client.sedex.create-directories:true}") boolean createDirectory)
      throws WatchDirNotAccessibleException {
    super(sedexConfiguration.getSedexReceiptPaths(), createDirectory);
    this.template = template;
  }

  protected void processFileEvent(@NonNull final FileEvent event) {
    log.info(event.toString());
    template.convertAndSend(Exchanges.LWGS, Topics.SEDEX_RECEIPTS, event);
  }
}
