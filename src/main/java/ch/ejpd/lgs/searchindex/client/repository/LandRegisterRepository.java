package ch.ejpd.lgs.searchindex.client.repository;

import ch.ejpd.lgs.searchindex.client.entity.LandRegister;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface LandRegisterRepository extends PagingAndSortingRepository<LandRegister, String> {
    List<LandRegister> getAllBySenderId(String senderId);

    void deleteAllBySenderId(String senderId);
}
