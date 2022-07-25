package org.datarocks.lwgs.searchindex.client.repository;

import java.util.Collection;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.entity.Log;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface LogRepository extends PagingAndSortingRepository<Log, Long> {
  Page<Log> findAll(@NonNull Pageable pageable);

  Page<Log> findAllBySourceIsInAndSeverityIsIn(
      @NonNull Collection<SourceType> filterSourceTypes,
      @NonNull Collection<SeverityType> filterSeverityTypes,
      @NonNull Pageable pageable);
}
