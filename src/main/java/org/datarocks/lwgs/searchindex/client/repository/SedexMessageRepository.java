package org.datarocks.lwgs.searchindex.client.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.entity.SedexMessage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface SedexMessageRepository extends CrudRepository<SedexMessage, UUID> {
  Optional<SedexMessage> findBySedexMessageId(@NonNull UUID sedexMessageId);

  Collection<SedexMessage> findAllByJobId(@NonNull UUID jobId);
}
