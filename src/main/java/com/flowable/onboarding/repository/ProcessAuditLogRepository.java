package com.flowable.onboarding.repository;

import com.flowable.onboarding.entity.ProcessAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProcessAuditLogRepository extends JpaRepository<ProcessAuditLog, Long> {

    List<ProcessAuditLog> findByProcessInstanceId(String processInstanceId);

    List<ProcessAuditLog> findByEmployeeId(Long employeeId);

    @Query("SELECT p FROM ProcessAuditLog p WHERE p.processInstanceId = :processInstanceId ORDER BY p.createdAt DESC")
    List<ProcessAuditLog> findByProcessInstanceIdOrderByCreatedAtDesc(@Param("processInstanceId") String processInstanceId);

    @Query("SELECT p FROM ProcessAuditLog p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<ProcessAuditLog> findAuditLogsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);

}
