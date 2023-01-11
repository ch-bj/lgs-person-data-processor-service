package org.datarocks.lwgs.searchindex.client.service.state;

import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.searchindex.client.entity.SedexMessage;
import org.datarocks.lwgs.searchindex.client.entity.SyncJob;
import org.datarocks.lwgs.searchindex.client.entity.type.JobState;
import org.datarocks.lwgs.searchindex.client.entity.type.SedexMessageState;
import org.datarocks.lwgs.searchindex.client.repository.SedexMessageRepository;
import org.datarocks.lwgs.searchindex.client.repository.SyncJobRepository;
import org.datarocks.lwgs.searchindex.client.service.amqp.CommonHeadersDao;
import org.datarocks.lwgs.searchindex.client.service.amqp.Queues;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SedexMessageStateProcessor {
  private final SedexMessageRepository sedexMessageRepository;
  private final SyncJobRepository syncJobRepository;

  @Autowired
  public SedexMessageStateProcessor(
      SedexMessageRepository sedexMessageRepository, SyncJobRepository syncJobRepository) {
    this.sedexMessageRepository = sedexMessageRepository;
    this.syncJobRepository = syncJobRepository;
  }

  @RabbitListener(queues = Queues.SEDEX_STATE)
  @Transactional
  public void listen(final Message message) {
    final CommonHeadersDao headers =
        new CommonHeadersDao(message.getMessageProperties().getHeaders());
    final UUID jobId = headers.getJobId();
    final Optional<SyncJob> optionalSyncJob = syncJobRepository.findByJobId(jobId);

    if (optionalSyncJob.isEmpty()) {
      log.warn(
          "Unable to find job with jobId {}. Skipping processing of state update message.", jobId);
      return;
    }

    final SyncJob syncJob = optionalSyncJob.get();
    final Set<SedexMessage> messages = new HashSet<>(sedexMessageRepository.findAllByJobId(jobId));
    final Optional<SedexMessage> lastMessage =
        messages.stream().filter(SedexMessage::isLastPage).findFirst();

    if (lastMessage.isPresent()
        && messages.size() == lastMessage.get().getPage() + 1
        && messages.stream()
            .map(SedexMessage::getState)
            .allMatch(SedexMessageState.SUCCESSFUL::equals)) {
      syncJob.setStateWithTimestamp(JobState.COMPLETED, Date.from(Instant.now()));
      syncJobRepository.save(syncJob);
    } else if (messages.stream()
        .map(SedexMessage::getState)
        .anyMatch(SedexMessageState.FAILED::equals)) {
      syncJob.setStateWithTimestamp(JobState.FAILED, Date.from(Instant.now()));
      syncJobRepository.save(syncJob);
    }
  }
}
