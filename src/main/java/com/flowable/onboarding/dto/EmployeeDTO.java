package com.flowable.onboarding.dto;

import com.flowable.onboarding.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {

    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Department is required")
    private String department;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm:ss[.SSS]]")
    private LocalDateTime joiningDate;

    private Long managerId;

    private Employee.OnboardingStatus status;

    private String processInstanceId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
