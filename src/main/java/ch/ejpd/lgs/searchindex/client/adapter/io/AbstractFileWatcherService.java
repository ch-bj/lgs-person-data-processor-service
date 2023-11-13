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

  /**
   * Constructor for AbstractFileWatcherService.
   * Initializes file watchers for the specified paths.
   *
   * @param paths              The list of paths to be watched.
   * @param createDirectories Flag indicating whether to create directories if they don't exist.
   * @throws WatchDirNotAccessibleException If setting up file watchers fails.
   */
  protected AbstractFileWatcherService(
      @NonNull final List<Path> paths, final boolean createDirectories)
      throws WatchDirNotAccessibleException {
    for (final Path path : paths) {
      fileWatchers.add(new FileWatcher(path, createDirectories));
    }
  }

  /**
   * Scheduled method to poll for file events.
   * Iterates through file watchers and processes file events.
   */
  @Scheduled(
      fixedDelayString = "${lwgs.searchindex.client.filewatcher.fixed-rate.in.milliseconds:1000}")
  public void scheduledPoll() {
    fileWatchers.forEach(watcher -> watcher.poll().forEach(this::processFileEvent));
  }

  /**
   * Abstract method to process a file event.
   *
   * @param event The file event to be processed.
   */
  abstract void processFileEvent(@NonNull final FileEvent event);
}
