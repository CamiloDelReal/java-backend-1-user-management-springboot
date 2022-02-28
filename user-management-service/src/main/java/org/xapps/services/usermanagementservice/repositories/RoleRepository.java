package org.xapps.services.usermanagementservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.xapps.services.usermanagementservice.entities.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Query(value = "SELECT * FROM roles WHERE id IN :ids", nativeQuery = true)
    List<Role> findByIds(List<Long> ids);
}
