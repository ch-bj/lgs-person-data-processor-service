package org.datarocks.lwgs.searchindex.client.repository;

import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface TransactionRepository extends PagingAndSortingRepository<Transaction, Long> {
  Optional<Transaction> findByTransactionId(@NonNull UUID transactionId);

  Page<Transaction> findAll(@NonNull Pageable pageable);

  Page<Transaction> findAllByJobId(@NonNull UUID jobId, @NonNull Pageable pageable);

  int countAllByJobId(@NonNull UUID jobId);
}
