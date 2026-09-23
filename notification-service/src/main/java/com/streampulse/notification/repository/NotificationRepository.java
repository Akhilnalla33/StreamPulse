package com.streampulse.notification.repository;

import com.streampulse.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByTenantIdOrderByFiredAtDesc(UUID tenantId, Pageable pageable);
}
