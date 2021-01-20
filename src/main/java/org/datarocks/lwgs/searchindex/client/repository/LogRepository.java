package org.datarocks.lwgs.searchindex.client.repository;

import java.util.Collection;
import org.datarocks.lwgs.searchindex.client.entity.Log;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface LogRepository extends PagingAndSortingRepository<Log, Long> {
  Page<Log> findAll(Pageable pageable);

  Page<Log> findAllBySourceIsInAndSeverityIsIn(
      Collection<SourceType> filterSourceTypes,
      Collection<SeverityType> filterSeverityTypes,
      Pageable pageable);
}
