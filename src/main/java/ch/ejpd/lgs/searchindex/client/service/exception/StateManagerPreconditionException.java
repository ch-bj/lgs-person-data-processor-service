package ch.ejpd.lgs.searchindex.client.service.exception;

/**
 * Exception thrown when a state manager encounters a precondition violation.
 */
public class StateManagerPreconditionException extends RuntimeException {
  public StateManagerPreconditionException() {}

  public StateManagerPreconditionException(String message) {
    super(message);
  }
}
