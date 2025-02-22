package com.processmonitor.repository;

import com.processmonitor.model.ExecutionLog;
import com.processmonitor.model.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, UUID> {

    Page<ExecutionLog> findByJobId(UUID jobId, Pageable pageable);

    Page<ExecutionLog> findByJobIdAndStatus(UUID jobId, JobStatus status, Pageable pageable);

    Page<ExecutionLog> findByStatus(JobStatus status, Pageable pageable);

    Page<ExecutionLog> findByStartedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<ExecutionLog> findByJobIdAndStartedAtBetween(UUID jobId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<ExecutionLog> findByJobIdAndStatusAndStartedAtBetween(UUID jobId, JobStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("SELECT e FROM ExecutionLog e WHERE e.status = :status AND e.startedAt < :before")
    List<ExecutionLog> findByStatusAndStartedAtBefore(@Param("status") JobStatus status,
                                                       @Param("before") LocalDateTime before);

    @Modifying
    @Query("DELETE FROM ExecutionLog e WHERE e.startedAt < :before")
    int deleteByStartedAtBefore(@Param("before") LocalDateTime before);

    long countByStatusAndStartedAtAfter(JobStatus status, LocalDateTime after);

    long countByStartedAtAfter(LocalDateTime after);

    @Query(value = """
            SELECT TO_CHAR(started_at, 'YYYY-MM-DD') AS date, COUNT(*) AS count
            FROM execution_logs
            WHERE started_at >= :since
            GROUP BY TO_CHAR(started_at, 'YYYY-MM-DD')
            ORDER BY TO_CHAR(started_at, 'YYYY-MM-DD')
            """, nativeQuery = true)
    List<Object[]> findDailyCountsSince(@Param("since") LocalDateTime since);

    @Query(value = """
            SELECT job_id,
                   COUNT(*) AS total,
                   SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
                   AVG(EXTRACT(EPOCH FROM (finished_at - started_at)) * 1000) AS avg_duration_ms
            FROM execution_logs
            WHERE finished_at IS NOT NULL
            GROUP BY job_id
            """, nativeQuery = true)
    List<Object[]> findJobPerformance();

    @Query(value = """
            SELECT job_id, COUNT(*) AS failure_count
            FROM execution_logs
            WHERE status = 'FAILED'
            GROUP BY job_id
            ORDER BY failure_count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findTopFailingJobs();

    List<ExecutionLog> findByJobIdAndStatus(UUID jobId, JobStatus status);

    @Query("SELECT e FROM ExecutionLog e WHERE e.jobId = :jobId ORDER BY e.startedAt DESC")
    Page<ExecutionLog> findLatestByJobId(@Param("jobId") UUID jobId, Pageable pageable);
}
