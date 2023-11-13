package ch.ejpd.lgs.searchindex.client.service.amqp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants class representing AMQP exchanges.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Exchanges {
  /**
   * The LWGS topic exchange.
   */
  public static final String LWGS = "lwgs.topic";

  /**
   * The LWGS state topic exchange.
   */
  public static final String LWGS_STATE = "lwgs.state.topic";
}
