package org.datarocks.lwgs.searchindex.client.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncJob {

  @Id @GeneratedValue private Long id;

  @Column(unique = true)
  private UUID jobId;

  private JobType jobType;
  private JobState jobState;

  private Date createdAt;
  private Date sendAt;
  private Date completedAt;
  private Date failedAt;

  private int numPersonMutations;
  private boolean hasErrors;

  @JsonManagedReference
  @OneToMany(
      mappedBy = "syncJob",
      targetEntity = Transaction.class,
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL)
  private List<Transaction> transactions;

  public void setStateWithTimestamp(JobState state, Date timestamp) {
    this.jobState = state;
    switch (state) {
      case NEW:
        this.createdAt = timestamp;
        break;
      case SENT:
        this.sendAt = timestamp;
        break;
      case COMPLETED:
        this.completedAt = timestamp;
        break;
      case FAILED_PROCESSING:
      case FAILED:
        this.failedAt = timestamp;
        break;
    }
  }
}
