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

  /**
   * Constructor for SedexFileWatcherService.
   *
   * @param sedexConfiguration Configuration for Sedex, including paths to be watched.
   * @param template           RabbitTemplate for sending Sedex file events to RabbitMQ.
   * @param createDirectory    Flag indicating whether to create directories if they don't exist.
   * @throws WatchDirNotAccessibleException If setting up file watchers fails.
   */
  @Autowired
  public SedexFileWatcherService(
      SedexConfiguration sedexConfiguration,
      RabbitTemplate template,
      @Value("${lwgs.searchindex.client.sedex.create-directories:true}") boolean createDirectory)
      throws WatchDirNotAccessibleException {
    super(sedexConfiguration.getSedexReceiptPaths(), createDirectory);
    this.template = template;
  }

  /**
   * Processes a file event by logging it and sending it to RabbitMQ.
   *
   * @param event The file event to be processed.
   */
  protected void processFileEvent(@NonNull final FileEvent event) {
    log.info(event.toString());
    template.convertAndSend(Exchanges.LWGS, Topics.SEDEX_RECEIPTS, event);
  }
}
