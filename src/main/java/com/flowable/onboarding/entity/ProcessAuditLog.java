package com.flowable.onboarding.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "process_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "AUDIT_LOG_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "process_instance_id", nullable = false)
    private String processInstanceId;

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(nullable = false)
    private String stage;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String performedBy;

    @Column(columnDefinition = "CLOB")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
