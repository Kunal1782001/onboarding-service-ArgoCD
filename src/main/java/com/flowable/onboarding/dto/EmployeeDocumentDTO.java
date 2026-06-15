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
public class EmployeeDocumentDTO {
    private Long id;
    private Long employeeId;
    private String documentType;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}
