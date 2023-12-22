package ch.ejpd.lgs.searchindex.client.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "land_register")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LandRegister {
    @Id
    @Column
    private String key;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "messages")
    private int messages;
}
