package com.flowable.onboarding.controller;

import com.flowable.onboarding.dto.ApiResponse;
import com.flowable.onboarding.dto.TaskDTO;
import com.flowable.onboarding.entity.OnboardingTask;
import com.flowable.onboarding.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "APIs for managing onboarding tasks")
public class TaskController {

    private final TaskService taskService;

    // ========================
    // Flowable Runtime Task Endpoints
    // ========================

    @GetMapping("/flowable/active")
    @Operation(summary = "Get all active Flowable tasks", description = "Get live BPMN user tasks from Flowable engine")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFlowableActiveTasks() {
        List<Map<String, Object>> tasks = taskService.getFlowableActiveTasks();
        return ResponseEntity.ok(new ApiResponse<>(200, "Active Flowable tasks retrieved", tasks));
    }

    @GetMapping("/flowable/process/{processInstanceId}")
    @Operation(summary = "Get Flowable tasks by process", description = "Get active BPMN tasks for a specific process")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFlowableTasksByProcess(
            @PathVariable String processInstanceId) {
        List<Map<String, Object>> tasks = taskService.getFlowableTasksByProcess(processInstanceId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Process tasks retrieved", tasks));
    }

    @GetMapping("/flowable/assignee/{assignee}")
    @Operation(summary = "Get Flowable tasks by assignee", description = "Get active tasks assigned to a user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFlowableTasksByAssignee(
            @PathVariable String assignee) {
        List<Map<String, Object>> tasks = taskService.getFlowableTasksByAssignee(assignee);
        return ResponseEntity.ok(new ApiResponse<>(200, "Assignee tasks retrieved", tasks));
    }

    @PostMapping("/flowable/{taskId}/complete")
    @Operation(summary = "Complete a Flowable task", description = "Complete a BPMN user task and advance the process")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeFlowableTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables,
            @RequestParam(required = false, defaultValue = "hr_admin") String completedBy) {
        if (variables == null) variables = new HashMap<>();
        Map<String, Object> result = taskService.completeFlowableTask(taskId, variables, completedBy);
        return ResponseEntity.ok(new ApiResponse<>(200, "Task completed, process advanced", result));
    }

    // ========================
    // Local DB Task Endpoints (backward compatible)
    // ========================

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Retrieve all onboarding tasks")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        ApiResponse<List<TaskDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Tasks retrieved successfully",
                tasks
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Retrieve task details by ID")
    public ResponseEntity<ApiResponse<TaskDTO>> getTaskById(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        ApiResponse<TaskDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Task retrieved successfully",
                task
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/process/{processInstanceId}")
    @Operation(summary = "Get tasks by process instance", description = "Retrieve all tasks for a specific process instance")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasksByProcessInstance(
            @PathVariable String processInstanceId) {
        List<TaskDTO> tasks = taskService.getTasksByProcessInstance(processInstanceId);
        ApiResponse<List<TaskDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Tasks retrieved successfully",
                tasks
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assignee/{assignedTo}")
    @Operation(summary = "Get tasks by assignee", description = "Retrieve all tasks assigned to a specific user")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasksByAssignee(@PathVariable String assignedTo) {
        List<TaskDTO> tasks = taskService.getTasksByAssignee(assignedTo);
        ApiResponse<List<TaskDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Tasks retrieved successfully",
                tasks
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assignee/{assignedTo}/pending")
    @Operation(summary = "Get pending tasks by assignee", description = "Retrieve pending tasks assigned to a specific user")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getPendingTasksByAssignee(@PathVariable String assignedTo) {
        List<TaskDTO> tasks = taskService.getPendingTasksByAssignee(assignedTo);
        ApiResponse<List<TaskDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Pending tasks retrieved successfully",
                tasks
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get tasks by status", description = "Retrieve tasks with a specific status")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasksByStatus(@PathVariable OnboardingTask.TaskStatus status) {
        List<TaskDTO> tasks = taskService.getTasksByStatus(status);
        ApiResponse<List<TaskDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Tasks retrieved successfully",
                tasks
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete task", description = "Mark a task as completed")
    public ResponseEntity<ApiResponse<TaskDTO>> completeTask(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        TaskDTO completedTask = taskService.completeTask(id, remarks);
        ApiResponse<TaskDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Task completed successfully",
                completedTask
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject task", description = "Mark a task as rejected")
    public ResponseEntity<ApiResponse<TaskDTO>> rejectTask(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        TaskDTO rejectedTask = taskService.rejectTask(id, remarks);
        ApiResponse<TaskDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Task rejected successfully",
                rejectedTask
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign task", description = "Assign a task to a different user")
    public ResponseEntity<ApiResponse<TaskDTO>> assignTask(
            @PathVariable Long id,
            @RequestParam String newAssignee) {
        TaskDTO assignedTask = taskService.assignTask(id, newAssignee);
        ApiResponse<TaskDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Task assigned successfully",
                assignedTask
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/process/{processInstanceId}/pending-count")
    @Operation(summary = "Get pending task count", description = "Get count of pending tasks for a process instance")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPendingTaskCount(
            @PathVariable String processInstanceId) {
        long count = taskService.getPendingTaskCount(processInstanceId);
        Map<String, Long> data = new HashMap<>();
        data.put("pendingCount", count);
        ApiResponse<Map<String, Long>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Pending task count retrieved successfully",
                data
        );
        return ResponseEntity.ok(response);
    }

}
