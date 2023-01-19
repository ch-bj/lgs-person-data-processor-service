package ch.ejpd.lgs.searchindex.client.entity;

import ch.ejpd.lgs.searchindex.client.entity.type.TransactionState;
import java.util.Date;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = @Index(columnList = "jobId"))
public class Transaction {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID transactionId;

  private Date createdAt;
  private Date updatedAt;
  private TransactionState state;
  private UUID jobId;
}
