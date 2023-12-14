package ch.ejpd.lgs.searchindex.client.model;

import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobMetaData {
  private final JobType type;
  private final UUID jobId;
  private final int pageNr;
  private final boolean isLastPage;
  private String landRegister;
  private final String version;
}
