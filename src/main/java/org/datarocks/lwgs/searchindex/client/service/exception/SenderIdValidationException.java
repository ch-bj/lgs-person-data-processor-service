package org.datarocks.lwgs.searchindex.client.service.exception;

public class SenderIdValidationException extends StateManagerPreconditionException {
  public SenderIdValidationException(String message) {
    super(message);
  }
}
