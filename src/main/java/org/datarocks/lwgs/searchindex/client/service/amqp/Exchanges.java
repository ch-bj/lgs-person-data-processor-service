package org.datarocks.lwgs.searchindex.client.service.amqp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Exchanges {
  public static final String LWGS = "lwgs.topic";

  public static final String LWGS_STATE = "lwgs.state.topic";

  public static final String LOG = "lwgs.log.topic";
}
