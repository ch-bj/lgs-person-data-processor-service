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

  /**
   * Adds seed data to the partial sync pool.
   *
   * @param request  The seed data request in JSON format.
   * @param senderId The sender ID (optional).
   * @return Response containing the transaction ID.
   */
  @PostMapping(path = "partial/person-data", consumes = MediaType.APPLICATION_JSON_VALUE)
  public TransactionIdResponse addSeedToPartialSyncPool(
      @RequestBody @NonNull String request,
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    return TransactionIdResponse.builder()
        .transactionId(jobSeedService.seedToPartial(request, senderId))
        .build();
  }

  /**
   * Retrieves partial sync queue statistics.
   *
   * @return Response containing the queue statistics.
   */
  @GetMapping(path = "partial/stats")
  public QueueStatsResponse partialSyncQueueStats() {
    return QueueStatsResponse.builder()
        .queuedMutations(jobSeedService.getPartialQueued())
        .processedMutations(jobSeedService.getPartialProcessed())
        .failedMutations(jobSeedService.getPartialFailed())
        .build();
  }

  /**
   * Adds seed data to the full sync pool.
   *
   * @param request  The seed data request in JSON format.
   * @param senderId The sender ID (optional).
   * @return Response containing the transaction ID, if it exists. Otherwise, throw a response stating that a full sync is necessary.
   */
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

  /**
   * Retrieves full sync queue statistics.
   * 
   * @return Response containing the queue statistics.
   */
  @GetMapping(path = "full/stats")
  public QueueStatsResponse fullSyncQueueStats() {
    return QueueStatsResponse.builder()
        .queuedMutations(jobSeedService.getFullQueued())
        .processedMutations(jobSeedService.getFullProcessed())
        .failedMutations(jobSeedService.getFullFailed())
        .build();
  }

  /**
   * Retrieves the current state of the full sync job.
   * 
   * @return Response containing the job ID and status
   */
  @GetMapping(path = "full/state")
  public FullSyncSeedStateResponse fullSyncState() {
    return FullSyncSeedStateResponse.builder()
        .jobId(fullSyncStateManager.getCurrentFullSyncJobId())
        .seedStatus(fullSyncStateManager.getFullSyncJobState())
        .build();
  }

  /**
   * Triggers the start of a full sync job.
   * 
   * @param senderId The sender ID (optional).
   * @return Response containing the sender ID, the job ID and the job status.
   */
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

  /**
   * Triggers the submission of a full sync job.
   * 
   * @param senderId The sender ID (optional).
   * @return Response containing the sender ID, the job ID and the job status.
   */
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

  /**
   * Triggers the reset of a full sync job.
   * 
   * @param force The reset of the sync job proceeds even if there are ongoing sync jobs which might get interrupted (optional).
   * @param senderId The sender ID (optional).
   * @return Response containing the sender ID and the job status.
   */
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

  /**
   * Retrieves a paginated list of sync jobs.
   * 
   * @param pageable Data will be displayed in pages (size per page is defined by the underlying data store).
   * @return Response containing the paginated list of sync jobs.
   */
  @GetMapping(path = "jobs")
  @PageableAsQueryParam
  public Page<SimpleSyncJobProjection> getJobs(
      @PageableDefault @Parameter(hidden = true) Pageable pageable) {
    return syncJobRepository.findAllProjectedBy(pageable);
  }

  /**
   * Retrieves details of a specific sync job.
   * 
   * @param jobId The ID of the sync job.
   * @return Response containing detailed job details.
   */
  @GetMapping(path = "job/{jobId}")
  @PageableAsQueryParam
  public SyncJob getJobForId(@PathVariable(name = "jobId") UUID jobId) {
    return syncJobRepository
        .findByJobId(jobId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /**
   * Retrieves transactions associated with a specific sync job.
   * 
   * @param jobId The ID of the sync job.
   * @param pageable Data will be displayed in pages (size per page is defined by the underlying data store).
   * @return Response containing the paginated list of transactions associated with the sync job
   */
  @GetMapping(path = "job/{jobId}/transactions")
  @PageableAsQueryParam
  public Page<Transaction> getJobForId(
      @PathVariable(name = "jobId") UUID jobId,
      @PageableDefault @Parameter(hidden = true) Pageable pageable) {
    return transactionRepository.findAllByJobId(jobId, pageable);
  }

  /**
   * Retrieves a paginated list of all transactions.
   * 
   * @param pageable Data will be displayed in pages (size per page is defined by the underlying data store).
   * @param senderId The sender ID (optional).
   * @return Response containing the paginated list of all transactions.
   */
  @GetMapping(path = "transactions")
  @PageableAsQueryParam
  public Page<Transaction> getTransactions(
      @PageableDefault @Parameter(hidden = true) Pageable pageable,
      @RequestHeader(value = Headers.X_LGS_SENDER_ID, required = false) String senderId) {
    return transactionRepository.findAll(pageable);
  }

  /**
   * Retrieves details of a specific transation.
   * 
   * @param transactionId The ID of the transaction.
   * @return Response containing the details of the specified transaction.
   */
  @GetMapping(path = "transaction/{transactionId}")
  public Transaction getTransaction(@PathVariable(name = "transactionId") UUID transactionId) {
    return transactionRepository
        .findByTransactionId(transactionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  /**
   * Retrieves a list of business validation logs associated with a specific transaction.
   *
   * @param transactionId The ID of the transaction.
   * @return A list of business validation logs for the specified transaction.
   */
  @GetMapping(path = "transaction/{transactionId}/logs")
  public List<BusinessValidationLog> getTransactionBusinessLogs(
      @PathVariable(name = "transactionId") UUID transactionId) {
    return businessLogRepository.findAllByTransactionId(transactionId);
  }
}
