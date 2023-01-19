package ch.ejpd.lgs.searchindex.client.service.amqp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Queues {
  public static final String SEDEX_RECEIPTS = "lwgs.sedex.recepits";
  public static final String SEDEX_OUTBOX = "lwgs.sedex.outbox";

  public static final String SEDEX_STATE = "lwgs.sedex.status";
  public static final String PERSONDATA_PARTIAL_INCOMING = "lwgs.persondata.partial.incoming";
  public static final String PERSONDATA_PARTIAL_OUTGOING = "lwgs.persondata.partial.outgoing";
  public static final String PERSONDATA_PARTIAL_FAILED = "lwgs.persondata.partial.failed";
  public static final String PERSONDATA_FULL_INCOMING = "lwgs.persondata.full.incoming";
  public static final String PERSONDATA_FULL_OUTGOING = "lwgs.persondata.full.outgoing";
  public static final String PERSONDATA_FULL_FAILED = "lwgs.persondata.full.failed";
  public static final String JOB_STATE = "lwgs.state.job";
  public static final String TRANSACTION_STATE = "lwgs.state.transaction";
  public static final String BUSINESS_LOG = "lwgs.state.business.log";
  public static final String LOGS = "lwgs.logs";
}
