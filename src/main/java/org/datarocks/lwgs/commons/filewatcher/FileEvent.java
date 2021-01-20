package org.datarocks.lwgs.commons.filewatcher;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class FileEvent implements Serializable {
  @NonNull String eventType;
  @NonNull String filename;
}
