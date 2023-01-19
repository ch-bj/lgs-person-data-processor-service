package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.BusinessValidationLog;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface BusinessLogRepository
    extends PagingAndSortingRepository<BusinessValidationLog, Long> {
  List<BusinessValidationLog> findAllByTransactionId(@NonNull UUID transactionId);
}
