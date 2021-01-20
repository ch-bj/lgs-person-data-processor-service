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

  public FullSyncService(RabbitTemplate template, FullSyncStateManager fullSyncStateManager1) {
    super(template);
    this.fullSyncStateManager = fullSyncStateManager1;
  }

  private boolean preCheckConditions() {
    return fullSyncStateManager.isStateSending()
        && fullSyncStateManager.isIncomingEmpty()
        && fullSyncStateManager.isFailedEmpty();
  }

  @Scheduled(fixedDelayString = "${lwgs.searchindex.client.sync.full.fixed-delay:1000}")
  public void fixedDelayFull() {
    if (preCheckConditions()) {
      processQueue(JobType.FULL, Queues.PERSONDATA_FULL_OUTGOING, Topics.SEDEX_OUTBOX);
      fullSyncStateManager.completedFullSync();
    } else {
      if (!fullSyncStateManager.isFailedEmpty() && !fullSyncStateManager.isStateFailed()) {
        fullSyncStateManager.failFullSync();
        log.warn("Failure queue is not empty, set current job to fail state.");
      }
      log.warn("Skipping full-sync-service run, pre-conditions failed.");
    }
  }
}
