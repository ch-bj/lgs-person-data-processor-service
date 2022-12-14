package org.datarocks.lwgs.searchindex.client.service.sync;

import static org.datarocks.lwgs.searchindex.client.service.sync.FullSyncStateManager.FULL_SYNC_STORED_JOB_ID_KEY;
import static org.datarocks.lwgs.searchindex.client.service.sync.FullSyncStateManager.FULL_SYNC_STORED_STATE_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.Setting;
import org.datarocks.lwgs.searchindex.client.repository.SettingRepository;
import org.datarocks.lwgs.searchindex.client.service.amqp.QueueStatsService;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.exception.StateChangeConflictingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

@TestMethodOrder(OrderAnnotation.class)
class FullSyncStateManagerTest {
  private final SettingRepository settingRepository = mock(SettingRepository.class);
  private final RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
  private final QueueStatsService queueStatsService = mock(QueueStatsService.class);
  private final FullSyncStateManager fullSyncStateManager =
      new FullSyncStateManager(settingRepository, queueStatsService, rabbitAdmin);

  @AfterEach
  void cleanup() {
    fullSyncStateManager.resetFullSync(true);
    reset(settingRepository);
    reset(rabbitAdmin);
  }

  @Test
  @Order(1)
  void loadSetting() {
    final UUID jobId = UUID.randomUUID();
    doReturn(
            Optional.of(
                Setting.builder()
                    .key(FULL_SYNC_STORED_STATE_KEY)
                    .value(FullSyncSeedState.SENDING.toString())
                    .build()))
        .when(settingRepository)
        .findByKey(FULL_SYNC_STORED_STATE_KEY);

    doReturn(
            Optional.of(
                Setting.builder().key(FULL_SYNC_STORED_JOB_ID_KEY).value(jobId.toString()).build()))
        .when(settingRepository)
        .findByKey(FULL_SYNC_STORED_JOB_ID_KEY);

    assertAll(
        () -> assertEquals(FullSyncSeedState.SENDING, fullSyncStateManager.getFullSyncJobState()),
        () -> assertEquals(jobId, fullSyncStateManager.getCurrentFullSyncJobId()));

    verify(settingRepository).findByKey(FULL_SYNC_STORED_STATE_KEY);
    verify(settingRepository).findByKey(FULL_SYNC_STORED_JOB_ID_KEY);
  }

  @Test
  void startFullSync() {
    assertEquals(FullSyncSeedState.READY, fullSyncStateManager.getFullSyncJobState());
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::completedFullSync);
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::submitFullSync);
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::failFullSync);
    assertThrows(
        StateChangeConflictingException.class, () -> fullSyncStateManager.resetFullSync(false));

    reset(settingRepository);

    fullSyncStateManager.startFullSync();

    verify(settingRepository, times(2)).save(any());

    assertEquals(FullSyncSeedState.SEEDING, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isStateSeeding());
  }

  @Test
  void submitFullSync() {
    fullSyncStateManager.startFullSync();
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::completedFullSync);
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::startFullSync);

    reset(settingRepository);

    fullSyncStateManager.submitFullSync();

    verify(settingRepository, times(1)).save(any());

    assertEquals(FullSyncSeedState.SENDING, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isStateSending());
  }

  @Test
  void resetFullSync() {
    fullSyncStateManager.startFullSync();
    fullSyncStateManager.submitFullSync();
    fullSyncStateManager.failFullSync();

    reset(settingRepository);

    fullSyncStateManager.resetFullSync(false);

    verify(settingRepository, times(1)).save(any());
    verify(rabbitAdmin, times(0)).purgeQueue(any());

    assertEquals(FullSyncSeedState.READY, fullSyncStateManager.getFullSyncJobState());
  }

  @Test
  void forceResetFullSync() {
    fullSyncStateManager.startFullSync();
    fullSyncStateManager.submitFullSync();
    fullSyncStateManager.failFullSync();

    reset(settingRepository);

    doReturn(0).when(queueStatsService).getQueueCount(Queues.PERSONDATA_FULL_INCOMING);
    doReturn(0).when(queueStatsService).getQueueCount(Queues.PERSONDATA_FULL_FAILED);

    fullSyncStateManager.resetFullSync(true);

    verify(settingRepository, times(1)).save(any());
    verify(rabbitAdmin, times(3)).purgeQueue(anyString(), anyBoolean());

    assertEquals(FullSyncSeedState.READY, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isIncomingEmpty());
    assertTrue(fullSyncStateManager.isFailedEmpty());
  }

  @Test
  void completedFullSync() {
    fullSyncStateManager.startFullSync();
    fullSyncStateManager.submitFullSync();

    reset(settingRepository);

    fullSyncStateManager.completedFullSync();

    verify(settingRepository, times(1)).save(any());

    assertEquals(FullSyncSeedState.COMPLETED, fullSyncStateManager.getFullSyncJobState());
  }

  @Test
  void failFullSync() {
    fullSyncStateManager.startFullSync();
    fullSyncStateManager.submitFullSync();
    reset(settingRepository);

    fullSyncStateManager.failFullSync();

    verify(settingRepository, times(1)).save(any());

    assertEquals(FullSyncSeedState.FAILED, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isStateFailed());
  }
}
