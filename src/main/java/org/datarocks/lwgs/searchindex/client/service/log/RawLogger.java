package org.datarocks.lwgs.searchindex.client.service.log;

import java.util.Date;
import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;

public interface RawLogger {
  void log(
      Date timestamp, SourceType source, SeverityType severity, String message, UUID correlationId);
}
