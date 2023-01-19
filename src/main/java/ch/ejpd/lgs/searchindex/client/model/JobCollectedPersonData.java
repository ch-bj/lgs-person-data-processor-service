package ch.ejpd.lgs.searchindex.client.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class JobCollectedPersonData implements Serializable {
  private @NonNull String senderId;
  private @NonNull UUID jobId;
  private @NonNull UUID messageId;
  private int page;
  private int numProcessed;
  private int numTotal;
  private @NonNull List<ProcessedPersonData> processedPersonDataList;
}
