package ch.ejpd.lgs.searchindex.client.entity.type;

/**
 * Enum representing different states of a transaction.
 */
public enum TransactionState {
  NEW,
  PROCESSED,
  FAILED,
  JOB_ASSOCIATED,
  JOB_SENT,
  JOB_RECEIVED
}
