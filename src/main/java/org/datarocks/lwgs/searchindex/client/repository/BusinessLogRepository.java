package org.datarocks.lwgs.searchindex.client.repository;

import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.entity.BusinessValidationLog;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface BusinessLogRepository
    extends PagingAndSortingRepository<BusinessValidationLog, Long> {
  List<BusinessValidationLog> findAllByTransactionId(@NonNull UUID transactionId);
}
