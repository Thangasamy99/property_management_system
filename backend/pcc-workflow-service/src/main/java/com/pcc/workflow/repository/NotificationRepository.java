package com.pcc.workflow.repository;

import com.pcc.workflow.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE n.recipientId = :uid "
            + "OR (n.recipientRole IS NOT NULL AND n.recipientRole IN :roles) "
            + "ORDER BY n.createdAt DESC")
    Page<Notification> visibleTo(@Param("uid") Long uid, @Param("roles") java.util.List<String> roles,
                                 Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.readFlag = false AND "
            + "(n.recipientId = :uid OR (n.recipientRole IS NOT NULL AND n.recipientRole IN :roles))")
    long unreadFor(@Param("uid") Long uid, @Param("roles") java.util.List<String> roles);

    @Query("SELECT n FROM Notification n WHERE n.id = :id AND (n.recipientId = :uid "
            + "OR (n.recipientRole IS NOT NULL AND n.recipientRole IN :roles))")
    Optional<Notification> findVisible(@Param("id") Long id, @Param("uid") Long uid,
                                       @Param("roles") java.util.List<String> roles);

    @Modifying
    @Query("UPDATE Notification n SET n.readFlag = true WHERE n.recipientId = :uid "
            + "OR (n.recipientRole IS NOT NULL AND n.recipientRole IN :roles)")
    int markAllRead(@Param("uid") Long uid, @Param("roles") java.util.List<String> roles);
}
