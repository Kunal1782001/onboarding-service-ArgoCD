package com.flowable.onboarding.repository;

import com.flowable.onboarding.entity.OnboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, Long> {

    Optional<OnboardingTask> findByFlowableTaskId(String flowableTaskId);

    List<OnboardingTask> findByProcessInstanceId(String processInstanceId);

    List<OnboardingTask> findByAssignedTo(String assignedTo);

    List<OnboardingTask> findByStatus(OnboardingTask.TaskStatus status);

    @Query("SELECT t FROM OnboardingTask t WHERE t.assignedTo = :assignedTo AND t.status = :status ORDER BY t.dueDate ASC")
    List<OnboardingTask> findPendingTasksByAssignee(@Param("assignedTo") String assignedTo, 
                                                     @Param("status") OnboardingTask.TaskStatus status);

    @Query("SELECT COUNT(t) FROM OnboardingTask t WHERE t.processInstanceId = :processInstanceId AND t.status != :status")
    long countPendingTasks(@Param("processInstanceId") String processInstanceId, 
                           @Param("status") OnboardingTask.TaskStatus status);

}
