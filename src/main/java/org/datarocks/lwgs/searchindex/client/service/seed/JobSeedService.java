package org.datarocks.lwgs.searchindex.client.service.seed;

import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.entity.type.TransactionState;
import org.datarocks.lwgs.searchindex.client.model.PersonData;
import org.datarocks.lwgs.searchindex.client.service.amqp.*;
import org.datarocks.lwgs.searchindex.client.service.sync.FullSyncStateManager;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JobSeedService {
  private final RabbitTemplate rabbitTemplate;
  private final QueueStatsService queueStatsService;
  private final FullSyncStateManager fullSyncStateManager;
  private static final String EMPTY_PAYLOAD = "";

  public JobSeedService(
      QueueStatsService queueStatsService,
      RabbitTemplate rabbitTemplate,
      FullSyncStateManager fullSyncStateManager) {
    this.queueStatsService = queueStatsService;
    this.rabbitTemplate = rabbitTemplate;
    this.fullSyncStateManager = fullSyncStateManager;
  }

  public UUID seedToPartial(String payload) {
    return seedToQueue(payload, Topics.PERSONDATA_PARTIAL_INCOMING, JobType.PARTIAL, null);
  }

  public UUID seedToFull(String payload) {
    if (fullSyncStateManager.isInStateSeeding()) {
      return seedToQueue(
          payload,
          Topics.PERSONDATA_FULL_INCOMING,
          JobType.FULL,
          fullSyncStateManager.getCurrentFullSyncJobId());
    }
    return null;
  }

  private UUID seedToQueue(String payload, String topicName, JobType jobType, UUID jobId) {
    final UUID transactionId = UUID.randomUUID();
    final CommonHeadersDao headers =
        CommonHeadersDao.builder()
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
