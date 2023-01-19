package ch.ejpd.lgs.searchindex.client.service.exception;

public class SenderIdValidationException extends StateManagerPreconditionException {
  public SenderIdValidationException(String message) {
    super(message);
  }
}
