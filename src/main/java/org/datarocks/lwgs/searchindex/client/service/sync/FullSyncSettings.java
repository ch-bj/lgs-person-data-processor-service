package org.datarocks.lwgs.searchindex.client.service.sync;

import static org.datarocks.lwgs.searchindex.client.service.sync.FullSyncSeedState.READY;

import lombok.Getter;
import lombok.NonNull;

@Getter
public enum FullSyncSettings {
  FULL_SYNC_STORED_STATE("full.sync.state", READY.toString()),
  FULL_SYNC_STORED_JOB_ID("full.sync.current.job.id", null),
  FULL_SYNC_STORED_PAGE("full.sync.outgoing.page", String.valueOf(-1)),
  FULL_SYNC_STORED_MESSAGE_TOTAL("full.sync.outgoing.messages.total", String.valueOf(0)),
  FULL_SYNC_STORED_MESSAGE_PROCESSED("full.sync.outgoing.messages.processed", String.valueOf(0));

  private final String key;

  private final String defaultValue;

  FullSyncSettings(@NonNull final String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  @Override
  public String toString() {
    return key;
  }
}
