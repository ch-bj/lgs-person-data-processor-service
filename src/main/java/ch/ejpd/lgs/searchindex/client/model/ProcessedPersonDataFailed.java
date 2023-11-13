package ch.ejpd.lgs.searchindex.client.model;

import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Model class representing processed person data with failure information.
 */
@Data
@Builder
public class ProcessedPersonDataFailed implements Serializable {
  private @NonNull String senderId;
  private @NonNull UUID transactionId;
  private @NonNull String payload;
  private @NonNull String failureReason;
}
