package ch.ejpd.lgs.searchindex.client.service.amqp;

public enum MessageCategory {
  TRANSACTION_EVENT,
  JOB_EVENT,
  BUSINESS_VALIDATION_LOG,
  UNKNOWN
}
