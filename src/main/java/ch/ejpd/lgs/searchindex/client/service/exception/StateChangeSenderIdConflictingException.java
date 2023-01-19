package ch.ejpd.lgs.searchindex.client.service.exception;

public class StateChangeSenderIdConflictingException extends StateManagerPreconditionException {
  public StateChangeSenderIdConflictingException(String message) {
    super(message);
  }
}
