package org.datarocks.lwgs.searchindex.client.service.amqp;

public class Queues {
  protected Queues() {}

  public static final String SEDEX_RECEIPTS = "lwgs.sedex.recepits";
  public static final String SEDEX_OUTBOX = "lwgs.sedex.outbox";
  public static final String PERSONDATA_PARTIAL_INCOMING = "lwgs.persondata.partial.incoming";
  public static final String PERSONDATA_PARTIAL_OUTGOING = "lwgs.persondata.partial.outgoing";
  public static final String PERSONDATA_PARTIAL_FAILED = "lwgs.persondata.partial.failed";
  public static final String PERSONDATA_FULL_INCOMING = "lwgs.persondata.full.incoming";
  public static final String PERSONDATA_FULL_OUTGOING = "lwgs.persondata.full.outgoing";
  public static final String PERSONDATA_FULL_FAILED = "lwgs.persondata.full.failed";
  public static final String JOB_STATE = "lwgs.job.state";
  public static final String LOGS = "lwgs.logs";
}
