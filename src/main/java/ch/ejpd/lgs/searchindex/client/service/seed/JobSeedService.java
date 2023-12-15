package ch.ejpd.lgs.searchindex.client.service.seed;

import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.entity.type.TransactionState;
import ch.ejpd.lgs.searchindex.client.model.PersonData;
import ch.ejpd.lgs.searchindex.client.service.amqp.*;
import ch.ejpd.lgs.searchindex.client.service.sync.FullSyncStateManager;
import ch.ejpd.lgs.searchindex.client.util.SenderIdUtil;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobSeedService {
  private final RabbitTemplate rabbitTemplate;
  private final QueueStatsService queueStatsService;
  private final FullSyncStateManager fullSyncStateManager;
  private static final String EMPTY_PAYLOAD = "";
  private final SenderIdUtil senderIdUtil;

  public JobSeedService(
      QueueStatsService queueStatsService,
      RabbitTemplate rabbitTemplate,
      FullSyncStateManager fullSyncStateManager,
      SedexConfiguration configuration,
      SenderIdUtil senderIdUtil) {
    this.queueStatsService = queueStatsService;
    this.rabbitTemplate = rabbitTemplate;
    this.fullSyncStateManager = fullSyncStateManager;
    this.senderIdUtil = senderIdUtil;
  }

  public UUID seedToPartial(@NonNull final String payload, final String senderId) {
    return seedToQueue(
        payload,
        Topics.PERSONDATA_PARTIAL_INCOMING,
        JobType.PARTIAL,
        null,
        senderIdUtil.getSenderId(senderId),
        null);
  }

  public UUID seedToFull(String payload, final String senderId) {
    if (fullSyncStateManager.isInStateSeeding()) {
      final UUID transactionId =
          seedToQueue(
              payload,
              Topics.PERSONDATA_FULL_INCOMING,
              JobType.FULL,
              fullSyncStateManager.getCurrentFullSyncJobId(),
              senderIdUtil.getSenderId(senderId),
              senderIdUtil.getRegionId(senderId));

      fullSyncStateManager.incFullSeedMessageCounter();
      return transactionId;
    }
    return null;
  }

  private UUID seedToQueue(
      final String payload,
      final String topicName,
      final JobType jobType,
      final UUID jobId,
      final String senderId,
      final String landRegister) {
    final UUID transactionId = UUID.randomUUID();
    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
            .senderId(senderId)
            .landRegister(landRegister)
            .jobType(jobType)
            .jobId(jobId)
            .messageCategory(MessageCategory.TRANSACTION_EVENT)
            .transactionState(TransactionState.NEW)
            .transactionId(transactionId)
            .timestamp()
            .build();

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS,
        topicName,
        PersonData.builder().transactionId(transactionId).payload(payload).build(),
        headers::applyAndSetTransactionIdAsCorrelationId);

    rabbitTemplate.convertAndSend(
        Exchanges.LWGS_STATE,
        topicName,
        EMPTY_PAYLOAD,
        headers::applyAndSetTransactionIdAsCorrelationId);
    return transactionId;
  }

  public int getPartialQueued() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_PARTIAL_INCOMING);
  }

  public int getPartialProcessed() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_PARTIAL_OUTGOING);
  }

  public int getPartialFailed() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_PARTIAL_FAILED);
  }

  public int getFullQueued() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_FULL_INCOMING);
  }

  public int getFullProcessed() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_FULL_OUTGOING);
  }

  public int getFullFailed() {
    return queueStatsService.getQueueCount(Queues.PERSONDATA_FULL_FAILED);
  }
}
