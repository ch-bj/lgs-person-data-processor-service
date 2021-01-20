package org.datarocks.lwgs.searchindex.client.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SedexConfiguration {
  @Value("${lwgs.searchindex.client.sedex.base-path}")
  private String sedexBasePath;

  @Value("${lwgs.searchindex.client.sedex.receipt-path}")
  private String sedexReceiptPath;

  @Value("${lwgs.searchindex.client.sedex.outbox-path}")
  private String sedexOutboxPath;

  @Value("${lwgs.searchindex.client.sedex.create-directories:false}")
  private boolean createDirectories;

  public Path getSedexReceiptPath() {
    return Paths.get(sedexBasePath, sedexReceiptPath).toAbsolutePath();
  }

  public Path getSedexOutboxPath() {
    return Paths.get(sedexBasePath, sedexOutboxPath).toAbsolutePath();
  }

  public boolean shouldCreateDirectories() {
    return createDirectories;
  }
}
