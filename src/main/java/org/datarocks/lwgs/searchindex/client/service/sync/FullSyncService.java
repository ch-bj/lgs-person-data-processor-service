package org.datarocks.lwgs.searchindex.client.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class FullSyncService extends AbstractSyncService {
  private final FullSyncStateManager fullSyncStateManager;

  public FullSyncService(RabbitTemplate template, FullSyncStateManager fullSyncStateManager) {
    super(template);
    this.fullSyncStateManager = fullSyncStateManager;
  }

  private boolean preCheckConditionsForStateSending() {
    return fullSyncStateManager.isInStateSeeded()
        && fullSyncStateManager.isIncomingQueueEmpty()
        && fullSyncStateManager.isFailedQueueEmpty();
  }

  private boolean preCheckConditionsForProcessing() {
    return fullSyncStateManager.isInStateSending()
        && fullSyncStateManager.isIncomingQueueEmpty()
        && fullSyncStateManager.isFailedQueueEmpty()
        && !fullSyncStateManager.isOutgoingQueueEmpty();
  }

  private boolean preCheckConditionsForStateCompleted() {
    return fullSyncStateManager.isInStateSending() && fullSyncStateManager.isOutgoingQueueEmpty();
  }

  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sync.full.fixed-delay:1000}")
  public void fixedDelayFull() {
    if (preCheckConditionsForStateSending()) {
      fullSyncStateManager.startSendingFullSync();
      // wait for next run, to ensure we're not missing any un-acked messages
      log.info(
          "Wait for next run to start processing queue full.outgoing [jobId: {}]",
          fullSyncStateManager.getCurrentFullSyncJobId());
    } else if (preCheckConditionsForProcessing()) {
      processQueue(
          JobType.FULL,
          Queues.PERSONDATA_FULL_OUTGOING,
          Topics.SEDEX_OUTBOX,
          fullSyncStateManager.getCurrentFullSyncJobId());
      // wait for next run, to ensure we're not missing any un-acked messages
      log.info(
          "Wait for next run to set full sync job state to completed [jobId: {}]",
          fullSyncStateManager.getCurrentFullSyncJobId());
    } else if (preCheckConditionsForStateCompleted()) {
      fullSyncStateManager.completedFullSync();
      log.info(
          "Completed full sync job [jobId: {}]", fullSyncStateManager.getCurrentFullSyncJobId());
    } else {
      if (!fullSyncStateManager.isFailedQueueEmpty() && !fullSyncStateManager.isInStateFailed()) {
        fullSyncStateManager.failFullSync();
        log.warn("Failure queue is not empty, set current job to fail state.");
      }
      log.debug("Skipping full-sync-service run, pre-conditions failed.");
    }
  }
}
