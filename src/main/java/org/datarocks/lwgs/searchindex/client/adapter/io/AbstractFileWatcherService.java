package org.datarocks.lwgs.searchindex.client.adapter.io;

import java.nio.file.Path;
import org.datarocks.lwgs.commons.filewatcher.FileEvent;
import org.datarocks.lwgs.commons.filewatcher.FileWatcher;
import org.datarocks.lwgs.commons.filewatcher.exception.WatchDirNotAccessibleException;
import org.springframework.scheduling.annotation.Scheduled;

public abstract class AbstractFileWatcherService {
  private final FileWatcher fileWatcher;

  protected AbstractFileWatcherService(final Path path, final boolean createDirectories)
      throws WatchDirNotAccessibleException {
    this.fileWatcher = new FileWatcher(path, createDirectories);
  }

  @Scheduled(
      fixedDelayString = "${lwgs.searchindex.client.filewatcher.fixed-rate.in.milliseconds:1000}")
  public void scheduledPoll() {
    this.fileWatcher.poll().forEach(this::processFileEvent);
  }

  abstract void processFileEvent(FileEvent event);
}
