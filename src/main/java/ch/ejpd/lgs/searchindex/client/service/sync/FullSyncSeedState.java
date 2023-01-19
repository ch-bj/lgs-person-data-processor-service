package ch.ejpd.lgs.searchindex.client.service.sync;

public enum FullSyncSeedState {
  READY,
  SEEDING,
  SEEDED,
  SENDING,
  COMPLETED,
  FAILED
}
