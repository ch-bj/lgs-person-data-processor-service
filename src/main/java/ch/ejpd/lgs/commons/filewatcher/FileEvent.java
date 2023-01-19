package ch.ejpd.lgs.commons.filewatcher;

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
