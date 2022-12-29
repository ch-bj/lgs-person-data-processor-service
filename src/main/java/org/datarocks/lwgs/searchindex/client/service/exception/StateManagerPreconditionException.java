package org.datarocks.lwgs.searchindex.client.service.exception;

public class StateManagerPreconditionException extends RuntimeException {
  public StateManagerPreconditionException() {}

  public StateManagerPreconditionException(String message) {
    super(message);
  }
}
