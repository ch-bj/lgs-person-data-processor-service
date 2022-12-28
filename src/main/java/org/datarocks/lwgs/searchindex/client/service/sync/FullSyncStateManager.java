package org.datarocks.lwgs.searchindex.client.service.sync;

import static org.datarocks.lwgs.searchindex.client.service.sync.FullSyncSeedState.*;
import static org.datarocks.lwgs.searchindex.client.service.sync.FullSyncSettings.*;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class FullSyncStateManager {
  private static final boolean BLOCKING_PURGE = false;

  private final AtomicReference<FullSyncSeedState> fullSyncSeedState = new AtomicReference<>(READY);
  private final AtomicReference<UUID> currentFullSyncJobId = new AtomicReference<>(null);
  private final AtomicInteger currentFullSyncPage = new AtomicInteger(0);
  private final AtomicInteger fullSyncMessagesTotal = new AtomicInteger(0);
  private final AtomicInteger fullSyncMessagesProcessed = new AtomicInteger(0);
  private final AtomicInteger currentFullSyncMessageCounter = new AtomicInteger(0);

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

    loadPersistedSettingsOrSystemDefaults();
  }

  @Transactional
  protected String loadPersistedSetting(@NonNull FullSyncSettings key) {
    return settingRepository
        .findByKey(key.toString())
        .map(Setting::getValue)
        .orElse(key.getDefaultValue());
  }

  @Transactional
  protected void persistSetting(@NonNull FullSyncSettings key, String value) {
    final Setting setting =
        settingRepository
            .findByKey(key.toString())
            .orElse(Setting.builder().key(key.toString()).build());
    setting.setValue(value);
    settingRepository.save(setting);
  }

  protected void loadPersistedSettingsOrSystemDefaults() {
    try {
      fullSyncSeedState.set(
          FullSyncSeedState.valueOf(loadPersistedSetting(FULL_SYNC_STORED_STATE)));
      final String jobId = loadPersistedSetting(FULL_SYNC_STORED_JOB_ID);
      if (jobId != null) {
        currentFullSyncJobId.set(UUID.fromString(jobId));
      }
      currentFullSyncPage.set(Integer.parseInt(loadPersistedSetting(FULL_SYNC_STORED_PAGE)));
      fullSyncMessagesTotal.set(
          Integer.parseInt(loadPersistedSetting(FULL_SYNC_STORED_MESSAGE_TOTAL)));
      fullSyncMessagesProcessed.set(
          Integer.parseInt(loadPersistedSetting(FULL_SYNC_STORED_MESSAGE_PROCESSED)));
    } catch (Exception e) {
      log.error("Failed to load defaults from db; reason: {}.", e.getMessage());
    }
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

  private void setFullSyncJobState(FullSyncSeedState state) {
    log.info(
        "Changed job state [{} -> {}] of full sync job [jobId {}]",
        fullSyncSeedState,
        state,
        currentFullSyncJobId);
    fullSyncSeedState.set(state);
    persistSetting(FULL_SYNC_STORED_STATE, state.toString());
  }

  public UUID getCurrentFullSyncJobId() {
    return currentFullSyncJobId.get();
  }

  private void setCurrentFullSyncJobId(UUID syncJobId) {
    currentFullSyncJobId.set(syncJobId);
    persistSetting(
        FULL_SYNC_STORED_JOB_ID, Optional.ofNullable(syncJobId).map(UUID::toString).orElse(null));
  }

  public FullSyncSeedState getFullSyncJobState() {
    return fullSyncSeedState.get();
  }

  private void resetCurrentFullSyncPage() {
    currentFullSyncPage.set(-1);
    persistSetting(FULL_SYNC_STORED_PAGE, "-1");
  }

  public Integer getCurrentFullSyncPage() {
    return currentFullSyncPage.get();
  }

  private void setFullSyncMessagesTotal(@NonNull Integer value) {
    fullSyncMessagesTotal.set(value);
    persistSetting(FULL_SYNC_STORED_MESSAGE_TOTAL, value.toString());
  }

  public Integer getFullSyncMessagesTotal() {
    return fullSyncMessagesTotal.get();
  }

  private void resetFullSyncMessagesProcessed() {
    fullSyncMessagesProcessed.set(0);
    persistSetting(FULL_SYNC_STORED_MESSAGE_PROCESSED, "0");
  }

  public Integer getFullSyncMessagesProcessed() {
    return fullSyncMessagesProcessed.get();
  }

  public void incNumMessagesProcessed(int numProcessed) {
    fullSyncMessagesProcessed.getAndAdd(numProcessed);
    persistSetting(
        FULL_SYNC_STORED_MESSAGE_PROCESSED, String.valueOf(fullSyncMessagesProcessed.get()));
  }

  public void incFullSeedMessageCounter() {
    currentFullSyncMessageCounter.getAndIncrement();
  }

  public void startFullSync() {
    if (Arrays.asList(COMPLETED, READY).contains(getFullSyncJobState())) {
      if (getFullSyncJobState() != READY) {
        resetFullSync(false);
      }
      setCurrentFullSyncJobId(UUID.randomUUID());
      setFullSyncJobState(SEEDING);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), SEEDING);
  }

  public void submitFullSync() {
    if (getFullSyncJobState() == SEEDING) {
      setFullSyncJobState(SEEDED);
      setFullSyncMessagesTotal(currentFullSyncMessageCounter.get());
      currentFullSyncMessageCounter.set(0);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), SEEDED);
  }

  public void startSendingFullSync() {
    if (getFullSyncJobState() == SEEDED) {
      setFullSyncJobState(SENDING);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), SENDING);
  }

  public void resetFullSync(boolean force) {
    if (force || Arrays.asList(COMPLETED, FAILED).contains(getFullSyncJobState())) {
      rabbitAdmin.purgeQueue(Queues.PERSONDATA_FULL_INCOMING, BLOCKING_PURGE);
      rabbitAdmin.purgeQueue(Queues.PERSONDATA_FULL_OUTGOING, BLOCKING_PURGE);
      rabbitAdmin.purgeQueue(Queues.PERSONDATA_FULL_FAILED, BLOCKING_PURGE);

      setCurrentFullSyncJobId(null);
      setFullSyncJobState(READY);
      setFullSyncMessagesTotal(0);
      resetCurrentFullSyncPage();
      resetFullSyncMessagesProcessed();
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), READY);
  }

  public void completedFullSync() {
    if (getFullSyncJobState() == SENDING) {
      setFullSyncJobState(COMPLETED);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), COMPLETED);
  }

  public void failFullSync() {
    if (Arrays.asList(SEEDING, SENDING, COMPLETED).contains(getFullSyncJobState())) {
      setFullSyncJobState(FAILED);
      return;
    }
    throw new StateChangeConflictingException(getFullSyncJobState(), COMPLETED);
  }

  public Integer getNextPage() {
    return currentFullSyncPage.incrementAndGet();
  }

  public Integer getCurrentPage() {
    return getCurrentFullSyncPage();
  }
}
