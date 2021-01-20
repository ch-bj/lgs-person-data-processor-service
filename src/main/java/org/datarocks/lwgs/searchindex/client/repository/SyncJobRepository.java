package org.datarocks.lwgs.searchindex.client.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.SimpleSyncJobProjection;
import org.datarocks.lwgs.searchindex.client.entity.SyncJob;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface SyncJobRepository extends PagingAndSortingRepository<SyncJob, Long> {
  Page<SyncJob> findAll(Pageable pageable);

  Page<SimpleSyncJobProjection> findAllProjectedBy(Pageable pageable);

  Page<SyncJob> findAllByJobType(JobType jobType, Pageable pageable);

  Page<SyncJob> findAllByJobTypeAndHasErrors(JobType jobType, boolean hasErrors, Pageable pageable);

  Optional<SyncJob> findByJobId(UUID jobId);

  List<SyncJob> findAllByJobId(UUID jobId);
}
