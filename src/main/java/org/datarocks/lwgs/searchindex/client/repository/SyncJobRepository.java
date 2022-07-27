package org.datarocks.lwgs.searchindex.client.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.entity.SimpleSyncJobProjection;
import org.datarocks.lwgs.searchindex.client.entity.SyncJob;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface SyncJobRepository extends PagingAndSortingRepository<SyncJob, Long> {
  Page<SyncJob> findAll(@NonNull Pageable pageable);

  Page<SimpleSyncJobProjection> findAllProjectedBy(@NonNull Pageable pageable);

  Page<SyncJob> findAllByJobType(@NonNull JobType jobType, @NonNull Pageable pageable);

  Page<SyncJob> findAllByJobTypeAndHasErrors(
      @NonNull JobType jobType, @NonNull boolean hasErrors, @NonNull Pageable pageable);

  Optional<SyncJob> findByJobId(@NonNull UUID jobId);

  List<SyncJob> findAllByJobId(@NonNull UUID jobId);
}
