package org.datarocks.lwgs.searchindex.client.entity;

import java.util.Date;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table
public class SyncJob {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID jobId;

  private JobType jobType;
  private JobState jobState;
  private Date createdAt;
  private Date sendAt;
  private Date completedAt;
  private Date failedAt;

  @Transient private int numPersonMutations;
  private boolean hasErrors;

  public void setStateWithTimestamp(JobState state, Date timestamp) {
    this.jobState = state;
    switch (state) {
      case NEW -> this.createdAt = timestamp;
      case SENT -> this.sendAt = timestamp;
      case COMPLETED -> this.completedAt = timestamp;
      case FAILED_PROCESSING, FAILED -> this.failedAt = timestamp;
      default -> {
        // nothing to do
      }
    }
  }
}
