package ch.ejpd.lgs.searchindex.client.service.state;

import ch.ejpd.lgs.searchindex.client.entity.SedexMessage;
import ch.ejpd.lgs.searchindex.client.entity.SyncJob;
import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.SedexMessageState;
import ch.ejpd.lgs.searchindex.client.repository.SedexMessageRepository;
import ch.ejpd.lgs.searchindex.client.repository.SyncJobRepository;
import ch.ejpd.lgs.searchindex.client.service.amqp.CommonHeadersDao;
import ch.ejpd.lgs.searchindex.client.service.amqp.Queues;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for processing and handling Sedex message state updates.
 */
@Service
@Slf4j
public class SedexMessageStateProcessor {
  private final SedexMessageRepository sedexMessageRepository;
  private final SyncJobRepository syncJobRepository;

  /**
   * Constructor for SedexMessageStateProcessor.
   * 
   * @param sedexMessageRepository Repository for storing Sedex message information.
   * @param syncJobRepository      Repository for storing synchronization job information.
   */
  @Autowired
  public SedexMessageStateProcessor(
      SedexMessageRepository sedexMessageRepository, SyncJobRepository syncJobRepository) {
    this.sedexMessageRepository = sedexMessageRepository;
    this.syncJobRepository = syncJobRepository;
  }

  /**
   * RabbitMQ listener for Sedex message state update messages.
   * 
   * @param message RabbitMQ message received from the SEDEX_STATE queue.
   */
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
