package org.datarocks.lwgs.searchindex.client.service.sync;

public enum FullSyncSeedState {
  READY,
  SEEDING,
  SEEDED,
  SENDING,
  COMPLETED,
  FAILED
}
