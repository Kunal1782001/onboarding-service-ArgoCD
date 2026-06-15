package com.flowable.onboarding.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
    @SequenceGenerator(name = "employee_seq", sequenceName = "EMPLOYEE_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String department;

    @Column(name = "joining_date", nullable = false)
    private LocalDateTime joiningDate;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OnboardingStatus status;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OnboardingStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OnboardingStatus {
        PENDING,           // Not yet started
        IN_PROGRESS,       // Onboarding in progress
        HR_APPROVAL,       // Waiting for HR approval
        MANAGER_APPROVAL,  // Waiting for manager approval
        IT_SETUP,          // IT is setting up systems
        ORIENTATION,       // Orientation stage
        COMPLETED,         // Onboarding completed
        REJECTED           // Application rejected
    }

}
