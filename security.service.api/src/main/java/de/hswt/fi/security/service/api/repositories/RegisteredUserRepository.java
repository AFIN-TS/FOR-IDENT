package de.hswt.fi.security.service.api.repositories;

import de.hswt.fi.security.service.model.RegisteredUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegisteredUserRepository extends CrudRepository<RegisteredUser, Long> {

	Optional<RegisteredUser> findByUsername(String username);

	Optional<RegisteredUser> findByMail(String mail);
}
