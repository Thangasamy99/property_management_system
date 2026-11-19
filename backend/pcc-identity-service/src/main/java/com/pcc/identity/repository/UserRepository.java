package com.pcc.identity.repository;

import com.pcc.identity.entity.User;
import com.pcc.identity.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findWithRolesById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeCode(String employeeCode);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByRoles_Code(String roleCode);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("""
            SELECT u FROM User u
            WHERE :q IS NULL
               OR LOWER(u.username) LIKE :q
               OR LOWER(u.email) LIKE :q
               OR LOWER(u.employeeCode) LIKE :q
               OR LOWER(u.firstName) LIKE :q
               OR LOWER(u.lastName) LIKE :q
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    long countByAccountStatus(AccountStatus status);

    List<User> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Page<User> findByRoles_Id(Long roleId, Pageable pageable);

    @Query("""
            SELECT CAST(u.createdAt AS LocalDate), COUNT(u)
            FROM User u
            WHERE u.createdAt >= :since
            GROUP BY CAST(u.createdAt AS LocalDate)
            """)
    List<Object[]> countByDaySince(@Param("since") java.time.Instant since);
}
