package ch.ejpd.lgs.commons.filewatcher.exception;

public class WatchDirNotAccessibleException extends Exception {
  public WatchDirNotAccessibleException(String path, Throwable cause) {
    super("Watchdir [" + path + "] not accessible.", cause);
  }
}
