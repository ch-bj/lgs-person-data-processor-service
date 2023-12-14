package ch.ejpd.lgs.searchindex.client.service.sync;

import static ch.ejpd.lgs.searchindex.client.service.sync.FullSyncSettings.FULL_SYNC_STORED_JOB_ID;
import static ch.ejpd.lgs.searchindex.client.service.sync.FullSyncSettings.FULL_SYNC_STORED_STATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.entity.Setting;
import ch.ejpd.lgs.searchindex.client.repository.SettingRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.QueueStatsService;
import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import ch.ejpd.lgs.searchindex.client.service.exception.StateChangeConflictingException;
import ch.ejpd.lgs.searchindex.client.service.exception.StateChangeSenderIdConflictingException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ch.ejpd.lgs.searchindex.client.util.SenderIdUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

@TestMethodOrder(OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class FullSyncStateManagerMultiSenderTest {

  private static final String SENDER_ID_A = "LGS-123-AAA";
  private static final String SENDER_ID_B = "LGS-123-BBB";
  @Mock private SettingRepository settingRepository;
  @Mock private RabbitAdmin rabbitAdmin;
  @Mock private QueueStatsService queueStatsService;

  @Mock private SedexConfiguration sedexConfiguration;
  @Mock private SenderIdUtil senderIdUtil;

  private FullSyncStateManager fullSyncStateManager;

  @BeforeEach
  void initialize() {
    when(sedexConfiguration.getSedexSenderId()).thenReturn(null);
    when(sedexConfiguration.getSedexSenderIds()).thenReturn(Set.of(SENDER_ID_A, SENDER_ID_B));
    when(sedexConfiguration.isInMultiSenderMode()).thenReturn(true);

    fullSyncStateManager =
        new FullSyncStateManager(
            settingRepository, queueStatsService, rabbitAdmin, senderIdUtil);
  }

  @AfterEach
  void cleanup() {
    fullSyncStateManager.resetFullSync(true, SENDER_ID_A);
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
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.submitFullSync(SENDER_ID_A));
    assertThrows(
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.submitFullSync(SENDER_ID_B));
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::failFullSync);
    assertThrows(
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.resetFullSync(false, SENDER_ID_A));
    assertThrows(
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.resetFullSync(false, SENDER_ID_B));

    reset(settingRepository);

    fullSyncStateManager.startFullSync(SENDER_ID_A);

    verify(settingRepository, atLeast(1)).save(any());

    assertEquals(FullSyncSeedState.SEEDING, fullSyncStateManager.getFullSyncJobState());
    assertEquals(SENDER_ID_A, fullSyncStateManager.getCurrentFullSyncSenderId());
    assertTrue(fullSyncStateManager.isInStateSeeding());
  }

  @Test
  void startFullSyncWithDifferentSenderId() {
    fullSyncStateManager.startFullSync(SENDER_ID_A);
    fullSyncStateManager.submitFullSync(SENDER_ID_A);
    fullSyncStateManager.startSendingFullSync();
    fullSyncStateManager.completedFullSync();

    reset(settingRepository);

    fullSyncStateManager.startFullSync(SENDER_ID_B);

    verify(settingRepository, atLeast(1)).save(any());

    assertEquals(FullSyncSeedState.SEEDING, fullSyncStateManager.getFullSyncJobState());
    assertEquals(SENDER_ID_B, fullSyncStateManager.getCurrentFullSyncSenderId());
    assertTrue(fullSyncStateManager.isInStateSeeding());

    // cleanup
    fullSyncStateManager.resetFullSync(true, SENDER_ID_B);
  }

  @Test
  void submitFullSync() {
    fullSyncStateManager.startFullSync(SENDER_ID_A);
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::completedFullSync);
    assertThrows(
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.startFullSync(SENDER_ID_A));
    assertThrows(
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.startFullSync(SENDER_ID_B));
    assertThrows(
        StateChangeSenderIdConflictingException.class,
        () -> fullSyncStateManager.submitFullSync(SENDER_ID_B));
    assertThrows(
        StateChangeSenderIdConflictingException.class,
        () -> fullSyncStateManager.submitFullSync(null));

    reset(settingRepository);

    fullSyncStateManager.submitFullSync(SENDER_ID_A);

    verify(settingRepository, atLeast(1)).save(any());

    assertEquals(FullSyncSeedState.SEEDED, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isInStateSeeded());
  }

  @Test
  void startSendingFullSync() {
    fullSyncStateManager.startFullSync(SENDER_ID_A);
    fullSyncStateManager.submitFullSync(SENDER_ID_A);
    assertThrows(StateChangeConflictingException.class, fullSyncStateManager::completedFullSync);
    assertThrows(
        StateChangeConflictingException.class,
        () -> fullSyncStateManager.startFullSync(SENDER_ID_A));

    reset(settingRepository);

    fullSyncStateManager.startSendingFullSync();

    verify(settingRepository, atLeast(1)).save(any());

    assertEquals(FullSyncSeedState.SENDING, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isInStateSending());
  }

  @Test
  void resetFullSync() {
    fullSyncStateManager.startFullSync(SENDER_ID_A);
    fullSyncStateManager.submitFullSync(SENDER_ID_A);
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
    fullSyncStateManager.startFullSync(SENDER_ID_A);
    fullSyncStateManager.submitFullSync(SENDER_ID_A);
    fullSyncStateManager.startSendingFullSync();
    fullSyncStateManager.failFullSync();

    doReturn(0).when(queueStatsService).getQueueCount(Queues.PERSONDATA_FULL_INCOMING);
    doReturn(0).when(queueStatsService).getQueueCount(Queues.PERSONDATA_FULL_FAILED);

    assertThrows(
        StateChangeSenderIdConflictingException.class,
        () -> fullSyncStateManager.resetFullSync(true, SENDER_ID_B));
    fullSyncStateManager.resetFullSync(true, SENDER_ID_A);

    verify(settingRepository, atLeast(1)).save(any());
    verify(rabbitAdmin, times(3)).purgeQueue(anyString(), anyBoolean());

    assertEquals(FullSyncSeedState.READY, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isIncomingQueueEmpty());
    assertTrue(fullSyncStateManager.isFailedQueueEmpty());
  }

  @Test
  void completedFullSync() {
    fullSyncStateManager.startFullSync(SENDER_ID_A);
    fullSyncStateManager.submitFullSync(SENDER_ID_A);
    fullSyncStateManager.startSendingFullSync();

    reset(settingRepository);

    fullSyncStateManager.completedFullSync();

    verify(settingRepository, times(1)).save(any());

    assertEquals(FullSyncSeedState.COMPLETED, fullSyncStateManager.getFullSyncJobState());
  }

  @Test
  void failFullSync() {
    fullSyncStateManager.startFullSync(SENDER_ID_A);
    fullSyncStateManager.submitFullSync(SENDER_ID_A);
    fullSyncStateManager.startSendingFullSync();

    reset(settingRepository);

    fullSyncStateManager.failFullSync();

    verify(settingRepository, times(1)).save(any());

    assertEquals(FullSyncSeedState.FAILED, fullSyncStateManager.getFullSyncJobState());
    assertTrue(fullSyncStateManager.isInStateFailed());
  }
}
