package org.datarocks.lwgs.searchindex.client.adapter.rest.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import org.datarocks.lwgs.searchindex.client.service.sync.FullSyncSeedState;

@Builder
@Data
public class FullSyncSeedStateResponse {
  String senderId;
  UUID jobId;
  FullSyncSeedState seedStatus;
}
