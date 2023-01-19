package ch.ejpd.lgs.searchindex.client.entity;

import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import java.util.Date;
import java.util.UUID;

public interface SimpleSyncJobProjection {
  Long getId();

  UUID getJobId();

  JobType getJobType();

  JobState getJobState();

  Date getCreatedAt();

  Date getSendAt();

  Date getCompletedAt();

  Date getFailedAt();

  int getNumPersonMutations();

  boolean getHasErrors();
}
