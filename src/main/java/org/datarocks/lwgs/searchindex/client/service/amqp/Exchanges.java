package org.datarocks.lwgs.searchindex.client.service.amqp;

public class Exchanges {
  protected Exchanges() {}

  public static final String LWGS = "lwgs.topic";

  public static final String LWGS_STATE = "lwgs.state.topic";

  public static final String LOG = "lwgs.log.topic";
}
