package ch.ejpd.lgs.searchindex.client.adapter.io;

import ch.ejpd.lgs.commons.filewatcher.FileEvent;
import ch.ejpd.lgs.commons.filewatcher.FileWatcher;
import ch.ejpd.lgs.commons.filewatcher.exception.WatchDirNotAccessibleException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.springframework.scheduling.annotation.Scheduled;

public abstract class AbstractFileWatcherService {
  private final List<FileWatcher> fileWatchers = new ArrayList<>();

  protected AbstractFileWatcherService(
      @NonNull final List<Path> paths, final boolean createDirectories)
      throws WatchDirNotAccessibleException {
    for (final Path path : paths) {
      fileWatchers.add(new FileWatcher(path, createDirectories));
    }
  }

  @Scheduled(
      fixedDelayString = "${lwgs.searchindex.client.filewatcher.fixed-rate.in.milliseconds:1000}")
  public void scheduledPoll() {
    fileWatchers.forEach(watcher -> watcher.poll().forEach(this::processFileEvent));
  }

  abstract void processFileEvent(@NonNull final FileEvent event);
}
