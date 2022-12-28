package org.datarocks.lwgs.searchindex.client.service.exception;

public class StateChangeSenderIdConflictingException extends RuntimeException {
  public StateChangeSenderIdConflictingException(String message) {
    super(message);
  }
}
