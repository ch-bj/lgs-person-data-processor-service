package ch.ejpd.lgs.searchindex.client.service.exception;

import ch.ejpd.lgs.searchindex.client.service.sync.FullSyncSeedState;

/**
 * Exception thrown when a conflicting state change is detected during full sync seed processing.
 */
public class StateChangeConflictingException extends StateManagerPreconditionException {
  private final FullSyncSeedState sourceState;
  private final FullSyncSeedState targetState;

  public StateChangeConflictingException(
      FullSyncSeedState sourceState, FullSyncSeedState targetState) {
    this.sourceState = sourceState;
    this.targetState = targetState;
  }

  @Override
  public String getMessage() {
    return "State change is not permitted ["
        + sourceState.toString()
        + "->"
        + targetState.toString()
        + "]";
  }
}
