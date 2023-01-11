package org.datarocks.lwgs.searchindex.client.entity;

import java.util.Date;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.entity.type.SedexMessageState;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = @Index(columnList = "sedexMessageId"))
public class SedexMessage {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID sedexMessageId;

  private String senderId;
  private Date createdAt;
  private Date updatedAt;
  private SedexMessageState state;
  private Integer page;
  private boolean isLastPage;
  private JobType jobType;
  private UUID jobId;
}
