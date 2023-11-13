package ch.ejpd.lgs.searchindex.client.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Sedex integration.
 */
@Configuration
public class SedexConfiguration {

  @Value("${lwgs.searchindex.client.sedex.file-writer.failure.throttling.base:1000}")
  @Getter
  private Long errorThrottlingBase;

  @Value("${lwgs.searchindex.client.sedex.file-writer.failure.throttling.max:600000}")
  @Getter
  private Long errorThrottlingMax;

  @Value("${lwgs.searchindex.client.sedex.base-path}")
  @Getter
  private String sedexBasePath;

  @Value("${lwgs.searchindex.client.sedex.receipt-path}")
  private String sedexReceiptPath;

  @Value("${lwgs.searchindex.client.sedex.outbox-path}")
  private String sedexOutboxPath;

  @Value("${lwgs.searchindex.client.sedex.sender-id}")
  private String sedexSenderId;

  @Value("${lwgs.searchindex.client.sedex.recipient-id}")
  @Getter
  private String sedexRecipientId;

  @Value("${lwgs.searchindex.client.sedex.message.type.full-export}")
  @Getter
  private int sedexMessageTypeFullExport;

  @Value("${lwgs.searchindex.client.sedex.message.type.incremental}")
  @Getter
  private int sedexMessageTypeIncremental;

  @Value("${lwgs.searchindex.client.sedex.message.class}")
  @Getter
  private int sedexMessageClass;

  @Value("${lwgs.searchindex.client.sedex.create-directories:false}")
  private boolean createDirectories;

  public boolean isInMultiSenderMode() {
    return sedexSenderId.contains(",");
  }

  public String getSedexSenderId() {
    if (!isInMultiSenderMode()) {
      return sedexSenderId;
    }
    return null;
  }

  public Set<String> getSedexSenderIds() {
    if (isInMultiSenderMode()) {
      return Set.of(sedexSenderId.split(","));
    } else return Collections.emptySet();
  }

  public Path getSedexReceiptPath() {
    return Paths.get(sedexBasePath, sedexReceiptPath).toAbsolutePath();
  }

  public Path getSedexOutboxPath() {
    return Paths.get(sedexBasePath, sedexOutboxPath).toAbsolutePath();
  }

  public Path getSedexReceiptPath(final String activeSedexSenderId) {
    return Paths.get(sedexBasePath, activeSedexSenderId, sedexReceiptPath).toAbsolutePath();
  }

  public List<Path> getSedexReceiptPaths() {
    return isInMultiSenderMode()
        ? getSedexSenderIds().stream()
            .map(senderId -> Paths.get(sedexBasePath, senderId, sedexReceiptPath).toAbsolutePath())
            .toList()
        : List.of(getSedexReceiptPath());
  }

  public Path getSedexOutboxPath(final String activeSedexSenderId) {
    return Paths.get(sedexBasePath, activeSedexSenderId, sedexOutboxPath).toAbsolutePath();
  }

  public boolean shouldCreateDirectories() {
    return createDirectories;
  }
}
