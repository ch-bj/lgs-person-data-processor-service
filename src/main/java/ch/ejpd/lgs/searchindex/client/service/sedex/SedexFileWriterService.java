package ch.ejpd.lgs.searchindex.client.service.sedex;

import ch.ejpd.lgs.searchindex.client.configuration.MavenPropertiesConfiguration;
import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.repository.SedexMessageRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SedexFileWriterService implements ThrottleHandler {
  private final SedexConfiguration configuration;
  private final SedexOutboxMessageProcessor outboxMessageProcessor;
  private boolean throttlingActive = false;
  private int errorCount = 0;
  private Instant retryTime = Instant.MIN;

  @Autowired
  public SedexFileWriterService(
      SedexConfiguration sedexConfiguration,
      RabbitTemplate rabbitTemplate,
      SedexMessageRepository sedexMessageRepository,
      MavenPropertiesConfiguration mavenPropertiesConfiguration) {

    this.configuration = sedexConfiguration;
    this.outboxMessageProcessor =
        new SedexOutboxMessageProcessor(
            rabbitTemplate,
            sedexConfiguration,
            sedexMessageRepository,
            this,
            mavenPropertiesConfiguration);
  }

  public void updateThrottling(boolean active) {
    if (active) {
      long waitingTime =
          Long.min(
              (long) Math.pow(2, errorCount) * configuration.getErrorThrottlingBase(),
              configuration.getErrorThrottlingMax());
      retryTime = Instant.now().plusMillis(waitingTime);
      errorCount++;
    } else {
      retryTime = Instant.MIN;
      errorCount = 0;
    }
    throttlingActive = active;
  }

  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sedex.file-writer.fixed-delay:60000}")
  @Async
  public void processSedexOutbox() {
    boolean loop;

    if (throttlingActive) {
      if (Instant.now().isBefore(retryTime)) {
        log.info(
            "Processing queue failed {} times, skip sedexFileWriter runs until {} (T-{}sec)",
            errorCount,
            retryTime,
            Duration.between(Instant.now(), retryTime).getSeconds());
        return;
      }
      log.info("Retry processing sedexFileWriter.");
    }

    do {
      loop = outboxMessageProcessor.processNextSedexOutboxMessage();
    } while (loop);

    updateThrottling(false);
  }
}
