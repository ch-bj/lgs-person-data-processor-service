package org.datarocks.lwgs.searchindex.client.service.log;

import java.util.Date;
import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;

public class SimpleLogger implements Logger {
  private final SourceType sourceType;
  private final RawLogger rawLogger;

  public SimpleLogger(SourceType sourceType, RawLogger rawLogger) {
    this.sourceType = sourceType;
    this.rawLogger = rawLogger;
  }

  @Override
  public void debug(Date timestamp, String message, UUID correlationId) {
    rawLogger.log(timestamp, sourceType, SeverityType.DEBUG, message, correlationId);
  }

  @Override
  public void info(Date timestamp, String message, UUID correlationId) {
    rawLogger.log(timestamp, sourceType, SeverityType.INFO, message, correlationId);
  }

  @Override
  public void warn(Date timestamp, String message, UUID correlationId) {
    rawLogger.log(timestamp, sourceType, SeverityType.WARN, message, correlationId);
  }

  @Override
  public void error(Date timestamp, String message, UUID correlationId) {
    rawLogger.log(timestamp, sourceType, SeverityType.ERROR, message, correlationId);
  }
}
