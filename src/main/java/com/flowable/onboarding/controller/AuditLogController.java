package com.flowable.onboarding.controller;

import com.flowable.onboarding.dto.ApiResponse;
import com.flowable.onboarding.dto.AuditLogDTO;
import com.flowable.onboarding.service.ProcessAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logging", description = "APIs for accessing audit logs")
public class AuditLogController {

    private final ProcessAuditService auditService;

    @GetMapping
    @Operation(summary = "Get all audit logs", description = "Retrieve all audit logs")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getAllAuditLogs() {
        List<AuditLogDTO> auditLogs = auditService.getAllAuditLogs();
        ApiResponse<List<AuditLogDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Audit logs retrieved successfully",
                auditLogs
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/process/{processInstanceId}")
    @Operation(summary = "Get audit logs by process instance", description = "Retrieve audit logs for a specific process instance")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getAuditLogsByProcessInstance(
            @PathVariable String processInstanceId) {
        List<AuditLogDTO> auditLogs = auditService.getAuditLogsByProcessInstance(processInstanceId);
        ApiResponse<List<AuditLogDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Audit logs retrieved successfully",
                auditLogs
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get audit logs by employee", description = "Retrieve audit logs for a specific employee")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getAuditLogsByEmployee(
            @PathVariable Long employeeId) {
        List<AuditLogDTO> auditLogs = auditService.getAuditLogsByEmployee(employeeId);
        ApiResponse<List<AuditLogDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Audit logs retrieved successfully",
                auditLogs
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get audit logs by date range", description = "Retrieve audit logs within a specific date range")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getAuditLogsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);

        List<AuditLogDTO> auditLogs = auditService.getAuditLogsByDateRange(start, end);
        ApiResponse<List<AuditLogDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Audit logs retrieved successfully",
                auditLogs
        );
        return ResponseEntity.ok(response);
    }

}
