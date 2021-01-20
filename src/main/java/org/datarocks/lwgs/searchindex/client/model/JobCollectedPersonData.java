package org.datarocks.lwgs.searchindex.client.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class JobCollectedPersonData implements Serializable {
  private @NonNull UUID jobId;
  private @NonNull List<ProcessedPersonData> processedPersonDataList;
}
