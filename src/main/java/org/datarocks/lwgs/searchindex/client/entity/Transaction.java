package org.datarocks.lwgs.searchindex.client.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import org.datarocks.lwgs.searchindex.client.entity.type.TransactionState;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

  @JsonIgnore @Id @GeneratedValue private Long id;

  @Column(unique = true)
  private UUID transactionId;

  private Date createdAt;
  private Date updatedAt;
  private TransactionState state;

  @JsonBackReference
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = SyncJob.class)
  @JoinColumn(name = "sync_job_id")
  private SyncJob syncJob;

  @Transient private UUID jobId;

  public UUID getJobId() {
    if (syncJob != null) {
      return syncJob.getJobId();
    }
    return null;
  }
}
