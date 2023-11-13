package ch.ejpd.lgs.searchindex.client.service.sync;

/**
 * Enum representing states in the full synchronization seed process.
 */
public enum FullSyncSeedState {
  READY,
  SEEDING,
  SEEDED,
  SENDING,
  COMPLETED,
  FAILED
}
