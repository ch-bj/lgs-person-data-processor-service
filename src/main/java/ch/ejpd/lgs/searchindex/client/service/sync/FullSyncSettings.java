package ch.ejpd.lgs.searchindex.client.service.sync;

import static ch.ejpd.lgs.searchindex.client.service.sync.FullSyncSeedState.READY;

import lombok.Getter;
import lombok.NonNull;

/**
 * Enum representing settings related to full synchronization.
 */
@Getter
public enum FullSyncSettings {
  FULL_SYNC_STORED_STATE("full.sync.state", READY.toString()),
  FULL_SYNC_STORED_SENDER_ID("full.sync.current.sender.id", null),
  FULL_SYNC_STORED_JOB_ID("full.sync.current.job.id", null),
  FULL_SYNC_STORED_PAGE("full.sync.outgoing.page", String.valueOf(-1)),
  FULL_SYNC_STORED_MESSAGE_TOTAL("full.sync.outgoing.messages.total", String.valueOf(0)),
  FULL_SYNC_STORED_MESSAGE_PROCESSED("full.sync.outgoing.messages.processed", String.valueOf(0));

  private final String key;

  private final String defaultValue;

  /**
   * Constructor for FullSyncSettings.
   *
   * @param key           The key associated with the setting.
   * @param defaultValue  The default value for the setting.
   */
  FullSyncSettings(@NonNull final String key, String defaultValue) {
    this.key = key;
    this.defaultValue = defaultValue;
  }

  /**
   * Returns the key of the setting.
   *
   * @return The key of the setting.
   */
  @Override
  public String toString() {
    return key;
  }
}
