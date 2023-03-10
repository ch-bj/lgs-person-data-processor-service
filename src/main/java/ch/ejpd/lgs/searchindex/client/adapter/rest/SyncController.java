package ch.ejpd.lgs.searchindex.client.adapter.rest;

import ch.ejpd.lgs.searchindex.client.adapter.rest.dto.FullSyncSeedStateResponse;
import ch.ejpd.lgs.searchindex.client.adapter.rest.dto.QueueStatsResponse;
import ch.ejpd.lgs.searchindex.client.adapter.rest.dto.TransactionIdResponse;
import ch.ejpd.lgs.searchindex.client.entity.BusinessValidationLog;
import ch.ejpd.lgs.searchindex.client.entity.SimpleSyncJobProjection;
import ch.ejpd.lgs.searchindex.client.entity.SyncJob;
import ch.ejpd.lgs.searchindex.client.entity.Transaction;
import ch.ejpd.lgs.searchindex.client.repository.BusinessLogRepository;
import ch.ejpd.lgs.searchindex.client.repository.SyncJobRepository;
import ch.ejpd.lgs.searchindex.client.repository.TransactionRepository;
import ch.ejpd.lgs.searchindex.client.service.exception.StateManagerPreconditionException;
import ch.ejpd.lgs.searchindex.client.service.seed.JobSeedService;
import ch.ejpd.lgs.searchindex.client.service.sync.FullSyncStateManager;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1/sync", produces = MediaType.APPLICATION_JSON_VALUE)
public class SyncController {
  private final JobSeedService jobSeedService;
  private final FullSyncStateManager fullSyncStateManager;
  private final SyncJobRepository syncJobRepository;
  private final TransactionRepository transactionRepository;
  private final BusinessLogRepository businessLogRepository;

  @Autowired
  public SyncController(
      JobSeedService jobSeedService,
      FullSyncStateManager fullSyncStateManager,
      SyncJobRepository syncJobRepository,
      TransactionRepository transactionRepository,
      BusinessLogRepository businessLogRepository) {
    this.jobSeedService = jobSeedService;
    this.fullSyncStateManager = fullSyncStateManager;
    this.syncJobRepository = syncJobRepository;
    this.transactionRepository = transactionRepository;
    this.businessLogRepository = businessLogRepository;
  }

  @PostMapping(path = "partial/person-data", consumes = MediaType.APPLICATION_JSON_VALUE)
  public TransactionIdResponse addSeedToPartialSyncPool(
      @RequestBody @NonNull String request,
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    return TransactionIdResponse.builder()
        .transactionId(jobSeedService.seedToPartial(request, senderId))
        .build();
  }

  @GetMapping(path = "partial/stats")
  public QueueStatsResponse partialSyncQueueStats() {
    return QueueStatsResponse.builder()
        .queuedMutations(jobSeedService.getPartialQueued())
        .processedMutations(jobSeedService.getPartialProcessed())
        .failedMutations(jobSeedService.getPartialFailed())
        .build();
  }

  @PostMapping(path = "full/person-data", consumes = MediaType.APPLICATION_JSON_VALUE)
  public TransactionIdResponse addSeedToFullSyncPool(
      @RequestBody @NonNull String request,
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    return TransactionIdResponse.builder()
        .transactionId(
            Optional.ofNullable(jobSeedService.seedToFull(request, senderId))
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.PRECONDITION_FAILED, "You need to start a fullSync first.")))
        .build();
  }

  @GetMapping(path = "full/stats")
  public QueueStatsResponse fullSyncQueueStats() {
    return QueueStatsResponse.builder()
        .queuedMutations(jobSeedService.getFullQueued())
        .processedMutations(jobSeedService.getFullProcessed())
        .failedMutations(jobSeedService.getFullFailed())
        .build();
  }

  @GetMapping(path = "full/state")
  public FullSyncSeedStateResponse fullSyncState() {
    return FullSyncSeedStateResponse.builder()
        .jobId(fullSyncStateManager.getCurrentFullSyncJobId())
        .seedStatus(fullSyncStateManager.getFullSyncJobState())
        .build();
  }

  @PutMapping(path = "full/trigger/start")
  public FullSyncSeedStateResponse triggerStartFullSync(
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    try {
      fullSyncStateManager.startFullSync(senderId);
    } catch (StateManagerPreconditionException e) {
      throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, e.getMessage());
    }
    return FullSyncSeedStateResponse.builder()
        .senderId(fullSyncStateManager.getCurrentFullSyncSenderId())
        .jobId(fullSyncStateManager.getCurrentFullSyncJobId())
        .seedStatus(fullSyncStateManager.getFullSyncJobState())
        .build();
  }

  @PutMapping(path = "full/trigger/submit")
  public FullSyncSeedStateResponse triggerSubmitFullSync(
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    try {
      fullSyncStateManager.submitFullSync(senderId);
    } catch (StateManagerPreconditionException e) {
      throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, e.getMessage());
    }
    return FullSyncSeedStateResponse.builder()
        .senderId(fullSyncStateManager.getCurrentFullSyncSenderId())
        .jobId(fullSyncStateManager.getCurrentFullSyncJobId())
        .seedStatus(fullSyncStateManager.getFullSyncJobState())
        .build();
  }

  @PutMapping(path = "full/trigger/reset")
  public FullSyncSeedStateResponse triggerResetFullSync(
      @RequestParam(required = false) boolean force,
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    try {
      fullSyncStateManager.resetFullSync(force, senderId);
    } catch (StateManagerPreconditionException e) {
      throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, e.getMessage());
    }
    return FullSyncSeedStateResponse.builder()
        .senderId(fullSyncStateManager.getCurrentFullSyncSenderId())
        .seedStatus(fullSyncStateManager.getFullSyncJobState())
        .build();
  }

  @GetMapping(path = "jobs")
  @PageableAsQueryParam
  public Page<SimpleSyncJobProjection> getJobs(
      @PageableDefault @Parameter(hidden = true) Pageable pageable) {
    return syncJobRepository.findAllProjectedBy(pageable);
  }

  @GetMapping(path = "job/{jobId}")
  @PageableAsQueryParam
  public SyncJob getJobForId(@PathVariable(name = "jobId") UUID jobId) {
    return syncJobRepository
        .findByJobId(jobId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping(path = "job/{jobId}/transactions")
  @PageableAsQueryParam
  public Page<Transaction> getJobForId(
      @PathVariable(name = "jobId") UUID jobId,
      @PageableDefault @Parameter(hidden = true) Pageable pageable) {
    return transactionRepository.findAllByJobId(jobId, pageable);
  }

  @GetMapping(path = "transactions")
  @PageableAsQueryParam
  public Page<Transaction> getTransactions(
      @PageableDefault @Parameter(hidden = true) Pageable pageable,
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    return transactionRepository.findAll(pageable);
  }

  @GetMapping(path = "transaction/{transactionId}")
  public Transaction getTransaction(@PathVariable(name = "transactionId") UUID transactionId) {
    return transactionRepository
        .findByTransactionId(transactionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping(path = "transaction/{transactionId}/logs")
  public List<BusinessValidationLog> getTransactionBusinessLogs(
      @PathVariable(name = "transactionId") UUID transactionId) {
    return businessLogRepository.findAllByTransactionId(transactionId);
  }
}
