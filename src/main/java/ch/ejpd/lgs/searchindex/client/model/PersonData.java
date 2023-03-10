package ch.ejpd.lgs.searchindex.client.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class PersonData implements Serializable {
  private @NonNull UUID transactionId;
  private @NonNull String payload;
}
