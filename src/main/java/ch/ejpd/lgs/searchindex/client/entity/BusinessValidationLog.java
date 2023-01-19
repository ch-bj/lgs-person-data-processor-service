package ch.ejpd.lgs.searchindex.client.entity;

import ch.ejpd.lgs.searchindex.client.entity.type.BusinessValidationEventType;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = @Index(columnList = "transactionId"))
public class BusinessValidationLog implements Serializable {

  @Id @GeneratedValue private Long id;

  private Date timestamp;
  private UUID transactionId;
  private BusinessValidationEventType event;

  @Column(length = 1000)
  private String message;
}
