package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.SimpleSyncJobProjection;
import ch.ejpd.lgs.searchindex.client.entity.SyncJob;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Repository for accessing synchronization job information.
 */
@RepositoryRestResource(exported = false)
@RepositoryRestResource(exported = false)
public interface SyncJobRepository extends PagingAndSortingRepository<SyncJob, UUID> {

  /**
   * Finds all synchronization jobs projected using the {@link SimpleSyncJobProjection}.
   *
   * @param pageable Pageable object for pagination
   */
  Page<SimpleSyncJobProjection> findAllProjectedBy(@NonNull Pageable pageable);

  /**
   * Finds all synchronization jobs of a specific type.
   *
   * @param jobType  The type of synchronization job
   * @param pageable Pageable object for pagination
   */
  Page<SyncJob> findAllByJobType(@NonNull JobType jobType, @NonNull Pageable pageable);

  /**
   * Finds all synchronization jobs of a specific type with or without errors.
   *
   * @param jobType   The type of synchronization job
   * @param hasErrors True if jobs with errors should be included, false otherwise
   * @param pageable  Pageable object for pagination
   */
  Page<SyncJob> findAllByJobTypeAndHasErrors(
      @NonNull JobType jobType, boolean hasErrors, @NonNull Pageable pageable);

  /**
   * Finds a synchronization job by its job ID.
   *
   * @param jobId The job ID
   */
  Optional<SyncJob> findByJobId(@NonNull UUID jobId);

  /**
   * Finds all synchronization jobs with a specific job ID.
   *
   * @param jobId The job ID
   */
  List<SyncJob> findAllByJobId(@NonNull UUID jobId);
}
