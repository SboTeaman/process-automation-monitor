package com.processmonitor.repository;

import com.processmonitor.model.Job;
import com.processmonitor.model.enums.JobStatus;
import com.processmonitor.model.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndDeletedFalse(UUID id);

    long countByDeletedFalse();

    Page<Job> findByDeletedFalse(Pageable pageable);

    Page<Job> findByTypeAndDeletedFalse(JobType type, Pageable pageable);

    Page<Job> findByLastStatusAndDeletedFalse(JobStatus lastStatus, Pageable pageable);

    Page<Job> findByCreatedByAndDeletedFalse(UUID createdBy, Pageable pageable);

    Page<Job> findByTypeAndLastStatusAndDeletedFalse(JobType type, JobStatus lastStatus, Pageable pageable);

    Page<Job> findByTypeAndCreatedByAndDeletedFalse(JobType type, UUID createdBy, Pageable pageable);

    Page<Job> findByLastStatusAndCreatedByAndDeletedFalse(JobStatus lastStatus, UUID createdBy, Pageable pageable);

    Page<Job> findByTypeAndLastStatusAndCreatedByAndDeletedFalse(
            JobType type, JobStatus lastStatus, UUID createdBy, Pageable pageable);

    List<Job> findByEnabledTrueAndDeletedFalse();

    @Query("SELECT j FROM Job j WHERE j.deleted = false AND j.enabled = true AND j.schedule IS NOT NULL")
    List<Job> findAllScheduledJobs();

    List<Job> findByTypeAndDeletedFalse(JobType type);

    List<Job> findByCreatedByAndDeletedFalse(UUID createdBy);
}
