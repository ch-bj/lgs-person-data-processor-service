package ch.ejpd.lgs.searchindex.client.service.exception;

/**
 * Exception thrown when the processing of person data fails.
 */
public class ProcessingPersonDataFailedException extends Exception {
  public ProcessingPersonDataFailedException(Exception e) {
    super(e);
  }
}
