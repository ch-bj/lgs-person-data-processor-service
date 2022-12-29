package org.datarocks.lwgs.searchindex.client.service.exception;

import org.datarocks.lwgs.searchindex.client.service.sync.FullSyncSeedState;

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
