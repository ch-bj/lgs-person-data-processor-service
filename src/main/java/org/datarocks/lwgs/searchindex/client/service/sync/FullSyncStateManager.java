package org.datarocks.lwgs.searchindex.client.service.sync;

import java.util.Arrays;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.Setting;
import org.datarocks.lwgs.searchindex.client.repository.SettingRepository;
import org.datarocks.lwgs.searchindex.client.service.amqp.QueueStatsService;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.exception.StateChangeConflictingException;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FullSyncStateManager {
  private static final String FULL_SYNC_STORED_STATE_KEY = "full.sync.state";
  private static final String FULL_SYNC_STORED_JOB_ID_KEY = "full.sync.current.job.id";
  private static final String FULL_SYNC_STORED_PAGE_KEY = "full.sync.outgoing.page";
  private static final boolean BLOCKING_PURGE = false;

  private static FullSyncSeedState fullSyncSeedState;
  private static volatile UUID currentFullSyncJobId;

  private static volatile Integer currentFullSyncPage;

  private final SettingRepository settingRepository;
  private final QueueStatsService queueStatsService;
  private final RabbitAdmin rabbitAdmin;

  @Autowired
  public FullSyncStateManager(
      SettingRepository settingRepository,
      QueueStatsService queueStatsService,
      RabbitAdmin rabbitAdmin) {
    this.settingRepository = settingRepository;
    this.queueStatsService = queueStatsService;
    this.rabbitAdmin = rabbitAdmin;
  }

  public boolean isInStateSeeding() {
    return getFullSyncJobState() == FullSyncSeedState.SEEDING;
  }

  public boolean isInStateSeeded() {
    return getFullSyncJobState() == FullSyncSeedState.SEEDED;
  }

  public boolean isInStateSending() {
    return getFullSyncJobState() == FullSyncSeedState.SENDING;
  }

  public boolean isInStateFailed() {
    return getFullSyncJobState() == FullSyncSeedState.FAILED;
  }

  public boolean isIncomingQueueEmpty() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_FULL_INCOMING) == 0;
  }

  public boolean isOutgoingQueueEmpty() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_FULL_OUTGOING) == 0;
  }

  public boolean isFailedQueueEmpty() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_FULL_FAILED) == 0;
  }

  public synchronized FullSyncSeedState getFullSyncJobState() {
    if (fullSyncSeedState == null) {
      // in case of a fresh start of the service, we'll try to restore the last setting from the db
      fullSyncSeedState =
          settingRepository
              .findByKey(FULL_SYNC_STORED_STATE_KEY)
              .map(Setting::getValue)
              .map(FullSyncSeedState::valueOf)
              .orElse(FullSyncSeedState.READY);
      currentFullSyncJobId =
          settingRepository
              .findByKey(FULL_SYNC_STORED_JOB_ID_KEY)
              .map(Setting::getValue)
              .map(UUID::fromString)
              .orElse(null);
    }
    return fullSyncSeedState;
  }

  private synchronized void setFullSyncJobState(FullSyncSeedState state) {
    // updated saved state in db
    Setting stateSetting =
        settingRepository
            .findByKey(FULL_SYNC_STORED_STATE_KEY)
            .orElse(Setting.builder().key(FULL_SYNC_STORED_STATE_KEY).build());
    stateSetting.setValue(state.toString());
    settingRepository.save(stateSetting);

    log.info(
        "Change job state [{} -> {}] of full sync job [jobId {}]",
        fullSyncSeedState,
        state,
        currentFullSyncJobId);
    fullSyncSeedState = state;
  }

  public synchronized UUID getCurrentFullSyncJobId() {
    return currentFullSyncJobId;
  }

  private synchronized void setCurrentFullSyncJobId(UUID syncJobId) {
    // updated saved jobId in db
    Setting jobIdSetting =
        settingRepository
            .findByKey(FULL_SYNC_STORED_JOB_ID_KEY)
            .orElse(Setting.builder().key(FULL_SYNC_STORED_JOB_ID_KEY).build());
    jobIdSetting.setValue(syncJobId.toString());

    settingRepository.save(jobIdSetting);

    currentFullSyncJobId = syncJobId;
  }

  private synchronized void setCurrentFullSyncPage(@NonNull Integer value) {
    Setting pageSetting =
        settingRepository
            .findByKey(FULL_SYNC_STORED_PAGE_KEY)
            .orElse(Setting.builder().key(FULL_SYNC_STORED_PAGE_KEY).build());
    pageSetting.setValue(value.toString());

    settingRepository.save(pageSetting);

    currentFullSyncPage = value;
  }

  public synchronized Integer getCurrentFullSyncPage() {
    if (currentFullSyncPage == null) {
      currentFullSyncPage =
          settingRepository
              .findByKey(FULL_SYNC_STORED_PAGE_KEY)
              .map(Setting::getValue)
              .map(Integer::valueOf)
              .orElse(-1);
    }
    return currentFullSyncPage;
  }

  public void startFullSync() {
    if (Arrays.asList(FullSyncSeedState.COMPLETED, FullSyncSeedState.READY)
        .contains(getFullSyncJobState())) {
      if (getFullSyncJobState() != FullSyncSeedState.READY) {
        resetFullSync(false);
      }
      setFullSyncJobState(FullSyncSeedState.SEEDING);
      setCurrentFullSyncJobId(UUID.randomUUID());
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), FullSyncSeedState.SEEDING);
  }

  public void submitFullSync() {
    if (getFullSyncJobState() == FullSyncSeedState.SEEDING) {
      setFullSyncJobState(FullSyncSeedState.SEEDED);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), FullSyncSeedState.SEEDED);
  }

  public void startSendingFullSync() {
    if (getFullSyncJobState() == FullSyncSeedState.SEEDED) {
      setFullSyncJobState(FullSyncSeedState.SENDING);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), FullSyncSeedState.SENDING);
  }

  public void resetFullSync(boolean force) {
    if (force
        || Arrays.asList(FullSyncSeedState.COMPLETED, FullSyncSeedState.FAILED)
            .contains(getFullSyncJobState())) {
      rabbitAdmin.purgeQueue(Queues.PERSONDATA_FULL_INCOMING, BLOCKING_PURGE);
      rabbitAdmin.purgeQueue(Queues.PERSONDATA_FULL_OUTGOING, BLOCKING_PURGE);
      rabbitAdmin.purgeQueue(Queues.PERSONDATA_FULL_FAILED, BLOCKING_PURGE);
      setFullSyncJobState(FullSyncSeedState.READY);
      setCurrentFullSyncPage(-1);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), FullSyncSeedState.READY);
  }

  public void completedFullSync() {
    if (getFullSyncJobState() == FullSyncSeedState.SENDING) {
      setFullSyncJobState(FullSyncSeedState.COMPLETED);
    }
  }

  public void failFullSync() {
    if (Arrays.asList(
            FullSyncSeedState.SEEDING, FullSyncSeedState.SENDING, FullSyncSeedState.COMPLETED)
        .contains(getFullSyncJobState())) {
      setFullSyncJobState(FullSyncSeedState.FAILED);
    }
  }

  public Integer getNextPage() {
    final Integer nextPage = getCurrentFullSyncPage() + 1;
    setCurrentFullSyncPage(nextPage);
    return nextPage;
  }

  public Integer getCurrentPage() {
    return getCurrentFullSyncPage();
  }
}
