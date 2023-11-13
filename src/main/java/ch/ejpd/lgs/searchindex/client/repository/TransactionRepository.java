package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.Transaction;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Repository for accessing transaction information.
 */
@RepositoryRestResource(exported = false)
public interface TransactionRepository extends PagingAndSortingRepository<Transaction, UUID> {

  /**
   * Finds a transaction by its transaction ID.
   *
   * @param transactionId The transaction ID
   */
  Optional<Transaction> findByTransactionId(@NonNull UUID transactionId);

  /**
   * Finds all transactions associated with a specific job ID.
   *
   * @param jobId    The job ID
   * @param pageable Pageable object for pagination
   */
  Page<Transaction> findAllByJobId(@NonNull UUID jobId, @NonNull Pageable pageable);

  /**
   * Counts the number of transactions associated with a specific job ID.
   *
   * @param jobId The job ID
   */
  int countAllByJobId(@NonNull UUID jobId);
}
