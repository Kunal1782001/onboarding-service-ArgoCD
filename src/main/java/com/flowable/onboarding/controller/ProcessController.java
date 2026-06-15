package com.flowable.onboarding.controller;

import com.flowable.onboarding.dto.ApiResponse;
import com.flowable.onboarding.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/processes")
@RequiredArgsConstructor
@Tag(name = "Process Management", description = "APIs for managing onboarding processes")
public class ProcessController {

    private final ProcessService processService;

    @PostMapping("/deploy")
    @Operation(summary = "Deploy process", description = "Deploy the employee onboarding BPMN process")
    public ResponseEntity<ApiResponse<String>> deployProcess() {
        processService.deployOnboardingProcess();
        ApiResponse<String> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Process deployed successfully",
                ProcessService.PROCESS_KEY
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start/{employeeId}")
    @Operation(summary = "Start onboarding process", description = "Start a new onboarding process for an employee")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startProcess(
            @PathVariable Long employeeId,
            @RequestBody(required = false) Map<String, Object> variables) {
        if (variables == null) {
            variables = new HashMap<>();
        }

        ProcessInstance processInstance = processService.startOnboardingProcess(employeeId, variables);

        Map<String, Object> response_data = new HashMap<>();
        response_data.put("processInstanceId", processInstance.getId());
        response_data.put("processDefinitionId", processInstance.getProcessDefinitionId());
        response_data.put("processKey", processInstance.getProcessDefinitionKey());

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Process started successfully",
                response_data
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{processInstanceId}")
    @Operation(summary = "Get process instance", description = "Retrieve process instance details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProcessInstance(
            @PathVariable String processInstanceId) {
        ProcessInstance processInstance = processService.getProcessInstance(processInstanceId);

        Map<String, Object> data = new HashMap<>();
        data.put("processInstanceId", processInstance.getId());
        data.put("processDefinitionId", processInstance.getProcessDefinitionId());
        data.put("processDefinitionKey", processInstance.getProcessDefinitionKey());
        data.put("isEnded", processInstance.isEnded());
        data.put("status", processService.getProcessInstanceStatus(processInstanceId));

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Process instance retrieved successfully",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all active processes", description = "Retrieve all active onboarding processes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllProcesses() {
        List<ProcessInstance> processes = processService.getAllActiveProcesses();

        List<Map<String, Object>> data = processes.stream().map(p -> {
            Map<String, Object> processData = new HashMap<>();
            processData.put("processInstanceId", p.getId());
            processData.put("processDefinitionKey", p.getProcessDefinitionKey());
            processData.put("isEnded", p.isEnded());
            return processData;
        }).collect(Collectors.toList());

        ApiResponse<List<Map<String, Object>>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Processes retrieved successfully",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{processInstanceId}/variables")
    @Operation(summary = "Get process variables", description = "Retrieve all variables for a process instance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProcessVariables(
            @PathVariable String processInstanceId) {
        Map<String, Object> variables = processService.getProcessVariables(processInstanceId);

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Process variables retrieved successfully",
                variables
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{processInstanceId}/variable/{variableName}")
    @Operation(summary = "Set process variable", description = "Set or update a process variable")
    public ResponseEntity<ApiResponse<String>> setProcessVariable(
            @PathVariable String processInstanceId,
            @PathVariable String variableName,
            @RequestBody Object variableValue) {
        processService.setProcessVariable(processInstanceId, variableName, variableValue);

        ApiResponse<String> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Process variable updated successfully",
                variableName
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{processInstanceId}/complete")
    @Operation(summary = "Complete process", description = "Complete a process instance administratively")
    public ResponseEntity<ApiResponse<String>> completeProcess(
            @PathVariable String processInstanceId,
            @RequestParam String completedBy,
            @RequestParam(required = false) String remarks) {
        processService.completeProcessInstance(processInstanceId, completedBy, remarks);

        ApiResponse<String> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Process completed successfully",
                processInstanceId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{processInstanceId}/reject/{employeeId}")
    @Operation(summary = "Reject process", description = "Reject an onboarding process")
    public ResponseEntity<ApiResponse<String>> rejectProcess(
            @PathVariable String processInstanceId,
            @PathVariable Long employeeId,
            @RequestParam String reason) {
        processService.rejectOnboardingProcess(processInstanceId, employeeId, reason);

        ApiResponse<String> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Process rejected successfully",
                processInstanceId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{processInstanceId}/status")
    @Operation(summary = "Get process status", description = "Get the current status of a process instance")
    public ResponseEntity<ApiResponse<Map<String, String>>> getProcessStatus(
            @PathVariable String processInstanceId) {
        String status = processService.getProcessInstanceStatus(processInstanceId);

        Map<String, String> data = new HashMap<>();
        data.put("status", status);

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Process status retrieved successfully",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{processInstanceId}/diagram", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get process diagram", description = "Generate a PNG flowchart of the running process with active tasks highlighted")
    public ResponseEntity<byte[]> getProcessDiagram(@PathVariable String processInstanceId) {
        byte[] diagram = processService.generateProcessDiagram(processInstanceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("no-cache");
        return new ResponseEntity<>(diagram, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/definition/diagram", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get process definition diagram", description = "Generate a PNG flowchart of the process definition (no active highlights)")
    public ResponseEntity<byte[]> getProcessDefinitionDiagram() {
        byte[] diagram = processService.generateProcessDefinitionDiagram();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("no-cache");
        return new ResponseEntity<>(diagram, headers, HttpStatus.OK);
    }

}
