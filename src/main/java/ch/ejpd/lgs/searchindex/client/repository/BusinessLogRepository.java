package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.BusinessValidationLog;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Repository for accessing business validation logs.
 */
@RepositoryRestResource(exported = false)
public interface BusinessLogRepository
    extends PagingAndSortingRepository<BusinessValidationLog, Long> {

  /**
   * Finds all business validation logs by transaction ID.
   *
   * @param transactionId The transaction ID
   */
  List<BusinessValidationLog> findAllByTransactionId(@NonNull UUID transactionId);
}
