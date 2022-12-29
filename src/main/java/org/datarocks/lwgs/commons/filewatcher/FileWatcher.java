package org.datarocks.lwgs.commons.filewatcher;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.commons.filewatcher.exception.WatchDirNotAccessibleException;

@Slf4j
public class FileWatcher {
  private final WatchService watcher;
  private final Path path;

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
