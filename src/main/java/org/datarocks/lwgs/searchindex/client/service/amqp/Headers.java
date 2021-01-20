package org.datarocks.lwgs.searchindex.client.service.amqp;

public class Headers {
  protected Headers() {}

  public static final String MESSAGE_CATEGORY = "MessageClass";
  public static final String TRANSACTION_ID = "TransactionID";
  public static final String TRANSACTION_STATE = "TransactionState";
  public static final String JOB_ID = "JobID";
  public static final String JOB_STATE = "JobState";
  public static final String JOB_TYPE = "JobType";
  public static final String TIMESTAMP = "Timestamp";
}
