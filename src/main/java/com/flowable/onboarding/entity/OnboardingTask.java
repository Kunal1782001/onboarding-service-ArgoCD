package com.flowable.onboarding.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "onboarding_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
    @SequenceGenerator(name = "task_seq", sequenceName = "ONBOARDING_TASK_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "process_instance_id", nullable = false)
    private String processInstanceId;

    @Column(name = "flowable_task_id", nullable = false, unique = true)
    private String flowableTaskId;

    @Column(nullable = false)
    private String taskName;

    @Column(nullable = false)
    private String assignedTo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(columnDefinition = "CLOB")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TaskStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TaskStatus {
        PENDING,      // Task assigned but not started
        IN_PROGRESS,  // Task is being worked on
        COMPLETED,    // Task completed
        REJECTED,     // Task rejected
        CANCELLED     // Task cancelled
    }

}
