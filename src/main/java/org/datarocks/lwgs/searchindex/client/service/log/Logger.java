package org.datarocks.lwgs.searchindex.client.service.log;

import java.util.Date;
import java.util.UUID;

public interface Logger {
  default void debug(String message) {
    debug(new Date(), message, null);
  }

  default void debug(String message, UUID correlationId) {
    debug(new Date(), message, correlationId);
  }

  void debug(Date timestamp, String message, UUID correlationId);

  default void info(String message) {
    info(new Date(), message, null);
  }

  default void info(String message, UUID correlationId) {
    info(new Date(), message, correlationId);
  }

  void info(Date timestamp, String message, UUID correlationId);

  default void warn(String message) {
    warn(new Date(), message, null);
  }

  default void warn(String message, UUID correlationId) {
    warn(new Date(), message, correlationId);
  }

  void warn(Date timestamp, String message, UUID correlationId);

  default void error(String message) {
    error(new Date(), message, null);
  }

  default void error(String message, UUID correlationId) {
    error(new Date(), message, correlationId);
  }

  void error(Date timestamp, String message, UUID correlationId);
}
