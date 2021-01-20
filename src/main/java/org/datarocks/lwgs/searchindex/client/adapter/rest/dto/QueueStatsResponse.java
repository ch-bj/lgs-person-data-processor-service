package org.datarocks.lwgs.searchindex.client.adapter.rest.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class QueueStatsResponse {
  int queuedMutations;
  int processedMutations;
  int failedMutations;
}
