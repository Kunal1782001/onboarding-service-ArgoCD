package com.flowable.onboarding.service;

import com.flowable.onboarding.dto.TaskDTO;
import com.flowable.onboarding.entity.Employee;
import com.flowable.onboarding.entity.OnboardingTask;
import com.flowable.onboarding.exception.ResourceNotFoundException;
import com.flowable.onboarding.repository.EmployeeRepository;
import com.flowable.onboarding.repository.OnboardingTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskService {

    private final OnboardingTaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;
    private final org.flowable.engine.TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final ProcessAuditService auditService;

    // ========================
    // Flowable Runtime Tasks
    // ========================

    /**
     * Get all active Flowable runtime tasks (the real process tasks)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFlowableActiveTasks() {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .orderByTaskCreateTime().desc()
                .list();
        return tasks.stream().map(this::mapFlowableTask).collect(Collectors.toList());
    }

    /**
     * Get active Flowable tasks for a specific process instance
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFlowableTasksByProcess(String processInstanceId) {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime().asc()
                .list();
        return tasks.stream().map(this::mapFlowableTask).collect(Collectors.toList());
    }

    /**
     * Get active Flowable tasks for a specific assignee
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFlowableTasksByAssignee(String assignee) {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();
        return tasks.stream().map(this::mapFlowableTask).collect(Collectors.toList());
    }

    /**
     * Complete a Flowable runtime task — this advances the BPMN process to the next stage
     */
    public Map<String, Object> completeFlowableTask(String taskId, Map<String, Object> variables, String completedBy) {
        Task task = flowableTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new ResourceNotFoundException("Flowable task not found with id: " + taskId);
        }

        String processInstanceId = task.getProcessInstanceId();
        String taskName = task.getName();
        String taskDefinitionKey = task.getTaskDefinitionKey();

        // Complete the task in Flowable engine (advances the process)
        if (variables != null && !variables.isEmpty()) {
            flowableTaskService.complete(taskId, variables);
        } else {
            flowableTaskService.complete(taskId);
        }

        log.info("Completed Flowable task: {} ({}) for process: {}", taskName, taskId, processInstanceId);

        // Update employee status based on which stage just completed
        updateEmployeeStatusAfterTask(processInstanceId, taskDefinitionKey);

        // Log audit
        Employee employee = employeeRepository.findByProcessInstanceId(processInstanceId).orElse(null);
        Long employeeId = employee != null ? employee.getId() : null;
        auditService.logAction(processInstanceId, employeeId, taskDefinitionKey, 
                "Task Completed: " + taskName, completedBy != null ? completedBy : "SYSTEM",
                "Task " + taskName + " completed successfully");

        // Check what tasks are now active after completion
        List<Map<String, Object>> nextTasks = getFlowableTasksByProcess(processInstanceId);

        // Check if process is now complete (no more tasks)
        boolean processComplete = nextTasks.isEmpty();
        if (processComplete && employee != null) {
            employee.setStatus(Employee.OnboardingStatus.COMPLETED);
            employeeRepository.save(employee);
            auditService.logAction(processInstanceId, employeeId, "COMPLETE", 
                    "Onboarding Complete", "SYSTEM", "All onboarding stages completed");
            log.info("Onboarding process COMPLETED for employee: {}", employeeId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("completedTask", taskName);
        result.put("completedTaskId", taskId);
        result.put("processInstanceId", processInstanceId);
        result.put("processComplete", processComplete);
        result.put("nextActiveTasks", nextTasks);
        return result;
    }

    private void updateEmployeeStatusAfterTask(String processInstanceId, String taskDefinitionKey) {
        Employee employee = employeeRepository.findByProcessInstanceId(processInstanceId).orElse(null);
        if (employee == null) return;

        switch (taskDefinitionKey) {
            case "ApplicationSubmission":
                employee.setStatus(Employee.OnboardingStatus.HR_APPROVAL);
                break;
            case "DocumentVerification":
            case "BackgroundCheck":
                // Only move to MANAGER_APPROVAL if both HR tasks are done
                List<Task> hrTasks = flowableTaskService.createTaskQuery()
                        .processInstanceId(processInstanceId).list();
                boolean onlyManagerOrLater = hrTasks.stream()
                        .noneMatch(t -> t.getTaskDefinitionKey().equals("DocumentVerification") 
                                     || t.getTaskDefinitionKey().equals("BackgroundCheck"));
                if (onlyManagerOrLater) {
                    employee.setStatus(Employee.OnboardingStatus.MANAGER_APPROVAL);
                }
                break;
            case "ManagerApproval":
                employee.setStatus(Employee.OnboardingStatus.IT_SETUP);
                break;
            case "EmailAccountCreation":
            case "SystemAccessSetup":
            case "HardwareProvisioning":
                // Only move to ORIENTATION if all IT tasks are done
                List<Task> itTasks = flowableTaskService.createTaskQuery()
                        .processInstanceId(processInstanceId).list();
                boolean onlyOrientationOrLater = itTasks.stream()
                        .noneMatch(t -> t.getTaskDefinitionKey().equals("EmailAccountCreation")
                                     || t.getTaskDefinitionKey().equals("SystemAccessSetup")
                                     || t.getTaskDefinitionKey().equals("HardwareProvisioning"));
                if (onlyOrientationOrLater) {
                    employee.setStatus(Employee.OnboardingStatus.ORIENTATION);
                }
                break;
            case "OrientationAssignment":
            case "FinalConfirmation":
                // Final confirmation means process virtually complete
                break;
        }
        employeeRepository.save(employee);
    }

    private Map<String, Object> mapFlowableTask(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", task.getId());
        map.put("taskName", task.getName());
        map.put("taskDefinitionKey", task.getTaskDefinitionKey());
        map.put("assignee", task.getAssignee());
        map.put("processInstanceId", task.getProcessInstanceId());
        map.put("createTime", task.getCreateTime());
        map.put("description", task.getDescription());

        // Add employee info
        try {
            Object empId = runtimeService.getVariable(task.getProcessInstanceId(), "employeeId");
            Object empName = runtimeService.getVariable(task.getProcessInstanceId(), "employeeName");
            map.put("employeeId", empId);
            map.put("employeeName", empName);
        } catch (Exception e) {
            map.put("employeeId", null);
            map.put("employeeName", "Unknown");
        }

        return map;
    }

    // ========================
    // Local Task DB Operations (kept for backward compatibility)
    // ========================

    public TaskDTO createTask(String processInstanceId, String flowableTaskId, String taskName, 
                             String assignedTo, String description) {
        OnboardingTask task = OnboardingTask.builder()
                .processInstanceId(processInstanceId)
                .flowableTaskId(flowableTaskId)
                .taskName(taskName)
                .assignedTo(assignedTo)
                .description(description)
                .status(OnboardingTask.TaskStatus.PENDING)
                .dueDate(LocalDateTime.now().plusDays(7)) // Default 7 days due date
                .build();

        OnboardingTask savedTask = taskRepository.save(task);
        return modelMapper.map(savedTask, TaskDTO.class);
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        OnboardingTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return modelMapper.map(task, TaskDTO.class);
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskByFlowableTaskId(String flowableTaskId) {
        OnboardingTask task = taskRepository.findByFlowableTaskId(flowableTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with Flowable ID: " + flowableTaskId));
        return modelMapper.map(task, TaskDTO.class);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProcessInstance(String processInstanceId) {
        return taskRepository.findByProcessInstanceId(processInstanceId).stream()
                .map(task -> modelMapper.map(task, TaskDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByAssignee(String assignedTo) {
        return taskRepository.findByAssignedTo(assignedTo).stream()
                .map(task -> modelMapper.map(task, TaskDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getPendingTasksByAssignee(String assignedTo) {
        return taskRepository.findPendingTasksByAssignee(assignedTo, OnboardingTask.TaskStatus.PENDING).stream()
                .map(task -> modelMapper.map(task, TaskDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(task -> modelMapper.map(task, TaskDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByStatus(OnboardingTask.TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(task -> modelMapper.map(task, TaskDTO.class))
                .collect(Collectors.toList());
    }

    public TaskDTO completeTask(Long id, String remarks) {
        OnboardingTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setStatus(OnboardingTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        OnboardingTask updatedTask = taskRepository.save(task);
        return modelMapper.map(updatedTask, TaskDTO.class);
    }

    public TaskDTO rejectTask(Long id, String remarks) {
        OnboardingTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setStatus(OnboardingTask.TaskStatus.REJECTED);
        OnboardingTask updatedTask = taskRepository.save(task);
        return modelMapper.map(updatedTask, TaskDTO.class);
    }

    public TaskDTO assignTask(Long id, String newAssignee) {
        OnboardingTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setAssignedTo(newAssignee);
        task.setStatus(OnboardingTask.TaskStatus.PENDING);

        OnboardingTask updatedTask = taskRepository.save(task);
        return modelMapper.map(updatedTask, TaskDTO.class);
    }

    public long getPendingTaskCount(String processInstanceId) {
        return taskRepository.countPendingTasks(processInstanceId, OnboardingTask.TaskStatus.COMPLETED);
    }

}
