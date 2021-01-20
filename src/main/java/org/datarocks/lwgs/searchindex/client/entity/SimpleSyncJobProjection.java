package org.datarocks.lwgs.searchindex.client.entity;

import java.util.Date;
import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;

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
