package ch.ejpd.lgs.commons.filewatcher;

import static java.nio.file.StandardWatchEventKinds.*;

import ch.ejpd.lgs.commons.filewatcher.exception.WatchDirNotAccessibleException;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Monitors a specified directory for file events (create, modify, delete) using the Java WatchService API.
 */
@Slf4j
public class FileWatcher {
  private final WatchService watcher;
  private final Path path;

  /**
   * Constructs a FileWatcher for the specified directory.
   *
   * @param path              The path to the directory to be watched.
   * @param createDirectories If true, attempt to create the directory if it does not exist.
   * @throws WatchDirNotAccessibleException If there is an issue accessing or setting up the WatchService.
   */
  public FileWatcher(@NonNull final Path path, boolean createDirectories)
      throws WatchDirNotAccessibleException {
    try {
      this.watcher = FileSystems.getDefault().newWatchService();
      File dir = path.toFile();
      if (createDirectories && !dir.exists() && !dir.mkdirs()) {
        log.warn("Directory creation failed.");
      }
      this.path = path;
      // ENTRY_DELETE, ENTRY_MODIFY
      final WatchKey key = path.register(watcher, ENTRY_CREATE);
      log.debug("File watcher initialised [key: {}].", key);
    } catch (IOException ioException) {
      log.error("Couldn't start fileWatcher, msg:", ioException);
      throw new WatchDirNotAccessibleException(path.toString(), ioException);
    }
  }

  /**
   * Processes a single WatchEvent and converts it into a FileEvent.
   *
   * @param event The WatchEvent to be processed.
   * @return The corresponding FileEvent or null if the event is an overflow.
   */
  private FileEvent processEvent(WatchEvent<?> event) {
    WatchEvent.Kind<?> kind = event.kind();

    if (kind == OVERFLOW) {
      log.error("File watcher dropped events, queue had been overflown.");
      return null;
    }

    WatchEvent<Path> ev = (WatchEvent<Path>) event;
    String filename = ev.context().toString();

    return FileEvent.builder()
        .filename(Paths.get(path.toString(), filename).toString())
        .eventType(kind.name())
        .build();
  }

  /**
   * Polls for file events and processes them into a list of FileEvent objects.
   *
   * @return A list of FileEvent objects representing the file events that occurred since the last poll.
   */
  public List<FileEvent> poll() {
    log.debug("polling.");

    if (watcher == null) {
      return Collections.emptyList();
    }

    final WatchKey key = watcher.poll();

    if (key == null) {
      return Collections.emptyList();
    }

    List<FileEvent> events =
        Optional.ofNullable(key.pollEvents()).orElse(Collections.emptyList()).stream()
            .map(this::processEvent)
            .filter(Objects::nonNull)
            .toList();

    key.reset();
    return events;
  }
}
