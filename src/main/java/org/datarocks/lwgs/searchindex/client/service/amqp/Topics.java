package org.datarocks.lwgs.searchindex.client.service.amqp;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Topics {
  public static final String SEDEX_RECEIPTS = "topics.sedex.recepits";
  public static final String SEDEX_OUTBOX = "topics.sedex.outbox";
  public static final String SEDEX_SENT = "topics.sedex.sent";
  public static final String SEDEX_RECEIVED = "topics.sedex.received";

  public static final String SEDEX_STATUS_UPDATED = "topics.sedex.state";
  public static final String PERSONDATA_PARTIAL_INCOMING = "topics.persondata.partial.incoming";
  public static final String PERSONDATA_PARTIAL_OUTGOING = "topics.persondata.partial.outgoing";
  public static final String PERSONDATA_PARTIAL_FAILED = "topics.persondata.partial.failed";
  public static final String PERSONDATA_FULL_INCOMING = "topics.persondata.full.incoming";
  public static final String PERSONDATA_FULL_OUTGOING = "topics.persondata.full.outgoing";
  public static final String PERSONDATA_FULL_FAILED = "topics.persondata.full.failed";
  public static final String PERSONDATA_BUSINESS_VALIDATION =
      "topics.persondata.business.validation";
  public static final String PERSONDATA_CATCH_ALL = "topics.persondata.#";
  public static final String CATCH_ALL = "#";
}
