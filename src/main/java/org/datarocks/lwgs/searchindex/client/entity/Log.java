package org.datarocks.lwgs.searchindex.client.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.*;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Log implements Serializable {

  @Id @GeneratedValue private Long id;

  private Date timestamp;
  private SourceType source;
  private SeverityType severity;
  private String message;
  private UUID transactionId;
  private UUID jobId;
}
