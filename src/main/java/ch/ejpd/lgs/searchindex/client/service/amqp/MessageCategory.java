package ch.ejpd.lgs.searchindex.client.service.amqp;

/**
 * Enumeration representing different message categories for AMQP messages.
 */
public enum MessageCategory {
  TRANSACTION_EVENT,
  JOB_EVENT,
  BUSINESS_VALIDATION_LOG,
  UNKNOWN
}
