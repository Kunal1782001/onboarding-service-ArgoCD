package com.flowable.onboarding.service;

import com.flowable.onboarding.dto.AuditLogDTO;
import com.flowable.onboarding.entity.ProcessAuditLog;
import com.flowable.onboarding.repository.ProcessAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProcessAuditService {

    private final ProcessAuditLogRepository auditLogRepository;
    private final ModelMapper modelMapper;

    public AuditLogDTO logAction(String processInstanceId, Long employeeId, String stage, 
                                 String action, String performedBy, String remarks) {
        ProcessAuditLog auditLog = ProcessAuditLog.builder()
                .processInstanceId(processInstanceId)
                .employeeId(employeeId)
                .stage(stage)
                .action(action)
                .performedBy(performedBy)
                .remarks(remarks)
                .build();

        ProcessAuditLog savedLog = auditLogRepository.save(auditLog);
        return modelMapper.map(savedLog, AuditLogDTO.class);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByProcessInstance(String processInstanceId) {
        return auditLogRepository.findByProcessInstanceIdOrderByCreatedAtDesc(processInstanceId).stream()
                .map(log -> modelMapper.map(log, AuditLogDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByEmployee(Long employeeId) {
        return auditLogRepository.findByEmployeeId(employeeId).stream()
                .map(log -> modelMapper.map(log, AuditLogDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findAuditLogsByDateRange(startDate, endDate).stream()
                .map(log -> modelMapper.map(log, AuditLogDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAllAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(log -> modelMapper.map(log, AuditLogDTO.class))
                .collect(Collectors.toList());
    }

}
