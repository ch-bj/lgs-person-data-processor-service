package ch.ejpd.lgs.searchindex.client.entity;

import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.entity.type.SedexMessageState;
import java.util.Date;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;

/**
 * Entity representing a Sedex message.
 */
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
