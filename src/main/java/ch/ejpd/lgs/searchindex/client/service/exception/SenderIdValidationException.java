package ch.ejpd.lgs.searchindex.client.service.exception;

/**
 * Exception thrown when the validation of the sender ID fails.
 */
public class SenderIdValidationException extends StateManagerPreconditionException {
  public SenderIdValidationException(String message) {
    super(message);
  }
}
