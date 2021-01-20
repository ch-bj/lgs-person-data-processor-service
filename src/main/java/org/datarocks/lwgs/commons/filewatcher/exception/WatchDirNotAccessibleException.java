package org.datarocks.lwgs.commons.filewatcher.exception;

public class WatchDirNotAccessibleException extends Exception {
  public WatchDirNotAccessibleException(String path, Throwable cause) {
    super("Watchdir [" + path + "] not accessible.", cause);
  }
}
