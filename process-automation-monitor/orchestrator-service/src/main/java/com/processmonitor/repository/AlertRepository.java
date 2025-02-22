package com.processmonitor.repository;

import com.processmonitor.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByAcknowledgedFalse(Pageable pageable);

    List<Alert> findByAcknowledgedFalse();

    Page<Alert> findByJobId(UUID jobId, Pageable pageable);

    long countByAcknowledgedFalse();
}
