package ch.ejpd.lgs.searchindex.client.service.exception;

/**
 * Exception thrown when a business validation error occurs.
 */
public class BusinessValidationException extends RuntimeException {
  /**
   * Constructs a new business validation exception with the specified detail message.
   *
   * @param message the detail message.
   */
  public BusinessValidationException(String message) {
    super(message);
  }
}
