package org.datarocks.lwgs.searchindex.client.service.sync;

public enum FullSyncSeedState {
  READY,
  SEEDING,
  SENDING,
  COMPLETED,
  FAILED
}
