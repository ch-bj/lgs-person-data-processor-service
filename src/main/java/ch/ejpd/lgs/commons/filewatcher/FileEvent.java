package ch.ejpd.lgs.commons.filewatcher;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Represents events related to a file (i.e. creation, modification, deletion).
 */
@Data
@Builder
public class FileEvent implements Serializable {
  @NonNull String eventType;
  @NonNull String filename;
}
