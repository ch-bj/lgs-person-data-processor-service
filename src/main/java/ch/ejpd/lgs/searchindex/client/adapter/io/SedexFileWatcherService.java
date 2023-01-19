package ch.ejpd.lgs.searchindex.client.adapter.io;

import ch.ejpd.lgs.commons.filewatcher.FileEvent;
import ch.ejpd.lgs.commons.filewatcher.exception.WatchDirNotAccessibleException;
import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.service.amqp.Exchanges;
import ch.ejpd.lgs.searchindex.client.service.amqp.Topics;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
