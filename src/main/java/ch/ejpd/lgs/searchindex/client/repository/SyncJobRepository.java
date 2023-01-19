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

@RepositoryRestResource(exported = false)
public interface SyncJobRepository extends PagingAndSortingRepository<SyncJob, UUID> {
  Page<SimpleSyncJobProjection> findAllProjectedBy(@NonNull Pageable pageable);

  Page<SyncJob> findAllByJobType(@NonNull JobType jobType, @NonNull Pageable pageable);

  Page<SyncJob> findAllByJobTypeAndHasErrors(
      @NonNull JobType jobType, boolean hasErrors, @NonNull Pageable pageable);

  Optional<SyncJob> findByJobId(@NonNull UUID jobId);

  List<SyncJob> findAllByJobId(@NonNull UUID jobId);
}
