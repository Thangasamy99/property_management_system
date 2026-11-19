package com.pcc.identity.repository;

import com.pcc.identity.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(Long id);
}
