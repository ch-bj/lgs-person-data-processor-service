package ch.ejpd.lgs.searchindex.client.service.sedex;

import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupSedexOutboxPathVerifier implements ApplicationListener<ContextRefreshedEvent> {

  private final SedexConfiguration configuration;
  private boolean processed;

  public StartupSedexOutboxPathVerifier(SedexConfiguration configuration) {
    this.configuration = configuration;
    processed = false;
  }

  private void verifyPath(@NonNull final String description, @NonNull final Path path) {
    final String metadata = String.format("[%sPath: %s]", description, path);
    if (!path.toFile().exists()) {
      if (configuration.shouldCreateDirectories()) {
        if (path.toFile().mkdirs()) {
          log.info("Created directory successfully {}", metadata);
        } else {
          log.error("Failed to create directory {}", metadata);
        }
      } else {
        log.error("Verification failed, {} directory not existing {}}", description, metadata);
      }
    } else {
      log.info("Verified {} directory successful {}", description, metadata);
    }
  }

  private void verifyPaths(@NonNull final Path outboxPath, @NonNull final Path receiptPath) {
    verifyPath("outbox", outboxPath);
    verifyPath("receipt", receiptPath);
    processed = true;
  }

  @Override
  public void onApplicationEvent(@NonNull final ContextRefreshedEvent event) {
    if (processed) {
      return;
    }

    if (configuration.isInMultiSenderMode()) {
      configuration
          .getSedexSenderIds()
          .forEach(
              senderId ->
                  verifyPaths(
                      configuration.getSedexOutboxPath(senderId),
                      configuration.getSedexReceiptPath(senderId)));
    } else {
      verifyPaths(configuration.getSedexOutboxPath(), configuration.getSedexReceiptPath());
    }
  }
}
