package ch.ejpd.lgs.searchindex.client.service.exception;

/**
 * Exception thrown when a conflicting state change is detected due to sender ID during processing.
 */
public class StateChangeSenderIdConflictingException extends StateManagerPreconditionException {
  public StateChangeSenderIdConflictingException(String message) {
    super(message);
  }
}
