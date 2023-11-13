package ch.ejpd.lgs.searchindex.client.service.exception;

/**
 * Exception thrown when writing Sedex files fails.
 */
public class WritingSedexFilesFailedException extends Exception {

  /**
   * Enum representing the different failure causes.
   */
  public enum FailureCause {
    DIRECTORY_CREATION_FAILED,
    FILE_CREATION_FAILED,
    FILE_WRITE_FAILED
  }

  /**
   * Constructs a new exception with the specified failure cause.
   *
   * @param failureCause The cause of the failure.
   */
  public WritingSedexFilesFailedException(FailureCause failureCause) {
    super("Writing sedex file failed with failure cause: " + failureCause.name());
  }
}
