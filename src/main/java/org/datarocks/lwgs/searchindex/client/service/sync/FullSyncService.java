package org.datarocks.lwgs.searchindex.client.service.sync;

import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.datarocks.lwgs.searchindex.client.service.amqp.Topics;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class FullSyncService extends AbstractSyncService {
  private final FullSyncStateManager fullSyncStateManager;

  public FullSyncService(
      RabbitTemplate template, FullSyncStateManager fullSyncStateManager, int pageSize) {
    super(template, pageSize);
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

  @Scheduled(
      fixedDelayString = "${lwgs.searchindex.client.sync.full.page-processor.fixed-delay:2000}")
  public void processNextPageOnQueueFullOutgoing() {
    if (preCheckConditionsForProcessing()) {
      int numProcessed =
          processFullQueuePaging(
              Queues.PERSONDATA_FULL_OUTGOING,
              Topics.SEDEX_OUTBOX,
              fullSyncStateManager.getCurrentFullSyncSenderId(),
              fullSyncStateManager.getCurrentFullSyncJobId(),
              fullSyncStateManager.getNextPage(),
              fullSyncStateManager.getFullSyncMessagesProcessed(),
              fullSyncStateManager.getFullSyncMessagesTotal());
      fullSyncStateManager.incNumMessagesProcessed(numProcessed);
    }
  }

  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sync.full.fixed-delay:1000}")
  public void fixedDelayFull() {
    if (preCheckConditionsForStateSending()) {
      fullSyncStateManager.startSendingFullSync();
      // wait for next run, to ensure we're not missing any un-acked messages
      log.info(
          "Wait for next run to start processing queue full.outgoing [jobId: {}, senderId: {}]",
          fullSyncStateManager.getCurrentFullSyncJobId(),
          fullSyncStateManager.getCurrentFullSyncSenderId());
    } else if (preCheckConditionsForProcessing()) {
      // in page processing..
      log.info(
          "Page wise processing is ongoing.. [jobId: {}, senderId: {}, current page: {}]",
          fullSyncStateManager.getCurrentFullSyncJobId(),
          fullSyncStateManager.getCurrentFullSyncSenderId(),
          fullSyncStateManager.getCurrentPage());
    } else if (preCheckConditionsForStateCompleted()) {
      fullSyncStateManager.completedFullSync();
      log.info(
          "Completed full sync job [jobId: {}, senderId: {}]",
          fullSyncStateManager.getCurrentFullSyncJobId(),
          fullSyncStateManager.getCurrentFullSyncSenderId());
    } else {
      if (!fullSyncStateManager.isFailedQueueEmpty() && !fullSyncStateManager.isInStateFailed()) {
        fullSyncStateManager.failFullSync();
        log.warn(
            "Failure queue is not empty, set current job to fail state [jobId: {}, senderId: {}]",
            fullSyncStateManager.getFullSyncJobState(),
            fullSyncStateManager.getCurrentFullSyncSenderId());
      }
      log.debug("Skipping full-sync-service run, pre-conditions failed.");
    }
  }
}
