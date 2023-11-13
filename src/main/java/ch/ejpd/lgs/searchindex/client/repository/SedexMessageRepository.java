package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.SedexMessage;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Repository for accessing Sedex messages.
 */
@RepositoryRestResource(exported = false)
public interface SedexMessageRepository extends CrudRepository<SedexMessage, UUID> {

  /**
   * Finds a Sedex message by its ID.
   *
   * @param sedexMessageId The Sedex message ID
   */
  Optional<SedexMessage> findBySedexMessageId(@NonNull UUID sedexMessageId);

  /**
   * Finds all Sedex messages by job ID.
   *
   * @param jobId The job ID
   */
  Collection<SedexMessage> findAllByJobId(@NonNull UUID jobId);
}
