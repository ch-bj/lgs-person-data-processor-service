package org.datarocks.lwgs.searchindex.client.entity.type;

public enum TransactionState {
  NEW,
  PROCESSED,
  FAILED,
  JOB_ASSOCIATED,
  JOB_SENT,
  JOB_RECEIVED
}
