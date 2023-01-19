package ch.ejpd.lgs.searchindex.client.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Setting {
  @Id
  @Column(unique = true)
  private String key;

  private String value;
}
