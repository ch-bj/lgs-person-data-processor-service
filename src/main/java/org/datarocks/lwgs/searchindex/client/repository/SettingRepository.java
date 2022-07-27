package org.datarocks.lwgs.searchindex.client.repository;

import java.util.Optional;
import lombok.NonNull;
import org.datarocks.lwgs.searchindex.client.entity.Setting;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface SettingRepository extends CrudRepository<Setting, String> {
  Optional<Setting> findByKey(@NonNull String key);
}
