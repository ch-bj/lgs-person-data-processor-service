package ch.ejpd.lgs.searchindex.client.service.exception;

public class WritingSedexFilesFailedException extends Exception {
  public enum FailureCause {
    DIRECTORY_CREATION_FAILED,
    FILE_CREATION_FAILED,
    FILE_WRITE_FAILED
  }

  public WritingSedexFilesFailedException(FailureCause failureCause) {
    super("Writing sedex file failed with failure cause: " + failureCause.name());
  }
}
