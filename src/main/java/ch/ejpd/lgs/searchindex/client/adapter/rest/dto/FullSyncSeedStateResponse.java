package ch.ejpd.lgs.searchindex.client.adapter.rest.dto;

import ch.ejpd.lgs.searchindex.client.service.sync.FullSyncSeedState;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FullSyncSeedStateResponse {
  String senderId;
  UUID jobId;
  FullSyncSeedState seedStatus;
}
