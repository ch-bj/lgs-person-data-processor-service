package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.Setting;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface SettingRepository extends CrudRepository<Setting, String> {
  Optional<Setting> findByKey(@NonNull String key);
}
