package ch.ejpd.lgs.commons.filewatcher.exception;

/**
 * Exception thrown when a directory specified for file watching is not accessible.
 */
public class WatchDirNotAccessibleException extends Exception {

  /**
   * Constructs a new WatchDirNotAccessibleException with a detail message indicating the inaccessible path.
   *
   * @param path   The path of the inaccessible directory.
   * @param cause  The cause of the exception (e.g. IO-exceptions).
   */
  public WatchDirNotAccessibleException(String path, Throwable cause) {
    super("Watchdir [" + path + "] not accessible.", cause);
  }
}
