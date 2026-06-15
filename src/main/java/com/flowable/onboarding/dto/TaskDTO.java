package com.flowable.onboarding.dto;

import com.flowable.onboarding.entity.OnboardingTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {

    private Long id;

    private String processInstanceId;

    private String flowableTaskId;

    private String taskName;

    private String assignedTo;

    private OnboardingTask.TaskStatus status;

    private LocalDateTime dueDate;

    private LocalDateTime completedAt;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
