package com.flowable.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {

    private Long id;

    private String processInstanceId;

    private Long employeeId;

    private String stage;

    private String action;

    private String performedBy;

    private String remarks;

    private LocalDateTime createdAt;

}
