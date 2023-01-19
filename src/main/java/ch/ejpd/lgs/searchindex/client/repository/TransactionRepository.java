package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.Transaction;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface TransactionRepository extends PagingAndSortingRepository<Transaction, UUID> {
  Optional<Transaction> findByTransactionId(@NonNull UUID transactionId);

  Page<Transaction> findAllByJobId(@NonNull UUID jobId, @NonNull Pageable pageable);

  int countAllByJobId(@NonNull UUID jobId);
}
