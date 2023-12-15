package ch.ejpd.lgs.searchindex.client.service.sync;

import static ch.ejpd.lgs.searchindex.client.service.sync.FullSyncSettings.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import ch.ejpd.lgs.searchindex.client.entity.Setting;
import ch.ejpd.lgs.searchindex.client.repository.SettingRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.QueueStatsService;
import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.service.exception.StateChangeConflictingException;
import ch.ejpd.lgs.searchindex.client.util.SenderIdUtil;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

@TestMethodOrder(OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class FullSyncStateManagerTest {
  private static final String SINGLE_SENDER_ID = "LGS-123-XYZ";
  @Mock private SettingRepository settingRepository;
  @Mock private RabbitAdmin rabbitAdmin;
  @Mock private QueueStatsService queueStatsService;

  @Mock private SenderIdUtil senderIdUtil;

  private FullSyncStateManager fullSyncStateManager;

  @BeforeEach
  void initialize() {
    fullSyncStateManager =
        new FullSyncStateManager(settingRepository, queueStatsService, rabbitAdmin, senderIdUtil);
  }

  @AfterEach
  void cleanup() {
    fullSyncStateManager.resetFullSync(true, null);
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
                    .key(FULL_SYNC_STORED_STATE.getKey())
                    .value(FullSyncSeedState.SENDING.toString())
                    .build()))
        .when(settingRepository)
        .findByKey(FULL_SYNC_STORED_STATE.getKey());

    doReturn(
            Optional.of(
                Setting.builder()
                    .key(FULL_SYNC_STORED_JOB_ID.getKey())
                    .value(jobId.toString())
                    .build()))
        .when(settingRepository)
        .findByKey(FULL_SYNC_STORED_JOB_ID.getKey());

    fullSyncStateManager.loadPersistedSettingsOrSystemDefaults();

    assertAll(
        () -> assertEquals(FullSyncSeedState.SENDING, fullSyncStateManager.getFullSyncJobState()),
        () -> assertEquals(jobId, fullSyncStateManager.getCurrentFullSyncJobId()));

    verify(settingRepository, times(2)).findByKey(FULL_SYNC_STORED_STATE.getKey());
    verify(settingRepository, times(2)).findByKey(FULL_SYNC_STORED_JOB_ID.getKey());
  }

  @Test
  void startFullSync() {
    assertEquals(FullSyncSeedState.READY, fullSyncStateManager.getFullSyncJobState());
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::completedFullSync);
    assertThrows(
        StateChangeConflictingException.class, () -> fullSyncStateManager.submitFullSync(null));
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::failFullSync);
    assertThrows(
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.resetFullSync(false, null));

    reset(settingRepository);

    fullSyncStateManager.startFullSync(null);

    verify(settingRepository, atLeast(1)).save(any());

    assertEquals(FullSyncSeedState.SEEDING, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isInStateSeeding());
  }

  @Test
  void submitFullSync() {
    when(senderIdUtil.getSenderId(isNull())).thenReturn(SINGLE_SENDER_ID);
    fullSyncStateManager.startFullSync(null);
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::completedFullSync);
    assertThrows(
        StateChangeConflictingException.class, () -> fullSyncStateManager.startFullSync(null));

    reset(settingRepository);

    fullSyncStateManager.submitFullSync(null);

    verify(settingRepository, atLeast(1)).save(any());

    assertEquals(FullSyncSeedState.SEEDED, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isInStateSeeded());
  }

  @Test
  void startSendingFullSync() {
    when(senderIdUtil.getSenderId(isNull())).thenReturn(SINGLE_SENDER_ID);
    fullSyncStateManager.startFullSync(null);
    fullSyncStateManager.submitFullSync(null);
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::completedFullSync);
    assertThrows(
        StateChangeConflictingException.class, () -> fullSyncStateManager.startFullSync(null));

    reset(settingRepository);

    fullSyncStateManager.startSendingFullSync();

    verify(settingRepository, atLeast(1)).save(any());

    assertEquals(FullSyncSeedState.SENDING, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isInStateSending());
  }

  @Test
  void resetFullSync() {
    when(senderIdUtil.getSenderId(isNull())).thenReturn(SINGLE_SENDER_ID);
    fullSyncStateManager.startFullSync(null);
    fullSyncStateManager.submitFullSync(null);
    fullSyncStateManager.startSendingFullSync();
    fullSyncStateManager.failFullSync();

    reset(settingRepository);

    fullSyncStateManager.resetFullSync(false, null);

    verify(settingRepository, atLeast(1)).save(any());
    verify(rabbitAdmin, times(0)).purgeQueue(any());

    assertEquals(FullSyncSeedState.READY, fullSyncStateManager.getFullSyncJobState());
  }

  @Test
  void forceResetFullSync() {
    when(senderIdUtil.getSenderId(isNull())).thenReturn(SINGLE_SENDER_ID);
    fullSyncStateManager.startFullSync(null);
    fullSyncStateManager.submitFullSync(null);
    fullSyncStateManager.startSendingFullSync();
    fullSyncStateManager.failFullSync();

    reset(settingRepository);

    doReturn(0).when(queueStatsService).getQueueCount(Queues.PERSONDATA_FULL_INCOMING);
    doReturn(0).when(queueStatsService).getQueueCount(Queues.PERSONDATA_FULL_FAILED);

    fullSyncStateManager.resetFullSync(true, null);

    verify(settingRepository, atLeast(1)).save(any());
    verify(rabbitAdmin, times(3)).purgeQueue(anyString(), anyBoolean());

    assertEquals(FullSyncSeedState.READY, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isIncomingQueueEmpty());
    assertTrue(fullSyncStateManager.isFailedQueueEmpty());
  }

  @Test
  void completedFullSync() {
    when(senderIdUtil.getSenderId(isNull())).thenReturn(SINGLE_SENDER_ID);
    fullSyncStateManager.startFullSync(null);
    fullSyncStateManager.submitFullSync(null);
    fullSyncStateManager.startSendingFullSync();

    reset(settingRepository);

    fullSyncStateManager.completedFullSync();

    verify(settingRepository, times(1)).save(any());

    assertEquals(FullSyncSeedState.COMPLETED, fullSyncStateManager.getFullSyncJobState());
  }

  @Test
  void failFullSync() {
    when(senderIdUtil.getSenderId(isNull())).thenReturn(SINGLE_SENDER_ID);
    fullSyncStateManager.startFullSync(null);
    fullSyncStateManager.submitFullSync(null);
    fullSyncStateManager.startSendingFullSync();
    reset(settingRepository);

    fullSyncStateManager.failFullSync();

    verify(settingRepository, times(1)).save(any());

    assertEquals(FullSyncSeedState.FAILED, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isInStateFailed());
  }
}
