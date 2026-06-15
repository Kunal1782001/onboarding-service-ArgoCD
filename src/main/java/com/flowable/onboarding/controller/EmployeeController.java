package com.flowable.onboarding.controller;

import com.flowable.onboarding.dto.ApiResponse;
import com.flowable.onboarding.dto.EmployeeDTO;
import com.flowable.onboarding.dto.EmployeeDocumentDTO;
import com.flowable.onboarding.entity.Employee;
import com.flowable.onboarding.service.EmployeeDocumentService;
import com.flowable.onboarding.service.EmployeeService;
import com.flowable.onboarding.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "APIs for managing employee records")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ProcessService processService;
    private final EmployeeDocumentService employeeDocumentService;

    @PostMapping
    @Operation(summary = "Create a new employee", description = "Create a new employee and initiate onboarding process")
    public ResponseEntity<ApiResponse<EmployeeDTO>> createEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO createdEmployee = employeeService.createEmployee(employeeDTO);

        // Start onboarding process
        try {
            Map<String, Object> variables = new HashMap<>();
            processService.startOnboardingProcess(createdEmployee.getId(), variables);
        } catch (Exception e) {
            // Log error but don't fail the employee creation
            System.err.println("Failed to start process: " + e.getMessage());
        }

        ApiResponse<EmployeeDTO> response = new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "Employee created successfully",
                createdEmployee
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all employees", description = "Retrieve all employees in the system")
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getAllEmployees() {
        List<EmployeeDTO> employees = employeeService.getAllEmployees();
        ApiResponse<List<EmployeeDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employees retrieved successfully",
                employees
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID", description = "Retrieve employee details by ID")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        EmployeeDTO employee = employeeService.getEmployeeById(id);
        ApiResponse<EmployeeDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employee retrieved successfully",
                employee
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get employee by email", description = "Retrieve employee details by email address")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeByEmail(@PathVariable String email) {
        EmployeeDTO employee = employeeService.getEmployeeByEmail(email);
        ApiResponse<EmployeeDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employee retrieved successfully",
                employee
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get employees by status", description = "Retrieve all employees with a specific onboarding status")
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getEmployeesByStatus(
            @PathVariable Employee.OnboardingStatus status) {
        List<EmployeeDTO> employees = employeeService.getEmployeesByStatus(status);
        ApiResponse<List<EmployeeDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employees retrieved successfully",
                employees
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department/{department}")
    @Operation(summary = "Get employees by department", description = "Retrieve all employees in a specific department")
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getEmployeesByDepartment(@PathVariable String department) {
        List<EmployeeDTO> employees = employeeService.getEmployeesByDepartment(department);
        ApiResponse<List<EmployeeDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employees retrieved successfully",
                employees
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Update employee details")
    public ResponseEntity<ApiResponse<EmployeeDTO>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO updatedEmployee = employeeService.updateEmployee(id, employeeDTO);
        ApiResponse<EmployeeDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employee updated successfully",
                updatedEmployee
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status/{status}")
    @Operation(summary = "Update employee status", description = "Update the onboarding status of an employee")
    public ResponseEntity<ApiResponse<EmployeeDTO>> updateEmployeeStatus(
            @PathVariable Long id,
            @PathVariable Employee.OnboardingStatus status) {
        EmployeeDTO updatedEmployee = employeeService.updateEmployeeStatus(id, status);
        ApiResponse<EmployeeDTO> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employee status updated successfully",
                updatedEmployee
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee", description = "Delete an employee record")
    public ResponseEntity<ApiResponse<String>> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        ApiResponse<String> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employee deleted successfully",
                null
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload employee documents", description = "Upload required onboarding documents for an employee")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadEmployeeDocuments(
            @PathVariable Long id,
            @RequestParam("aadhaar") MultipartFile aadhaar,
            @RequestParam("pan") MultipartFile pan,
            @RequestParam("uan") MultipartFile uan,
            @RequestParam("payslip1") MultipartFile payslip1,
            @RequestParam("payslip2") MultipartFile payslip2,
            @RequestParam("payslip3") MultipartFile payslip3,
            @RequestParam("bankStatement") MultipartFile bankStatement) {

        Map<String, MultipartFile> documents = new HashMap<>();
        documents.put("aadhaar", aadhaar);
        documents.put("pan", pan);
        documents.put("uan", uan);
        documents.put("payslip1", payslip1);
        documents.put("payslip2", payslip2);
        documents.put("payslip3", payslip3);
        documents.put("bankStatement", bankStatement);

        Map<String, String> uploaded = employeeDocumentService.uploadEmployeeDocuments(id, documents);

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employee documents uploaded successfully",
                uploaded
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "Get employee documents", description = "Retrieve all uploaded documents for an employee")
    public ResponseEntity<ApiResponse<List<EmployeeDocumentDTO>>> getEmployeeDocuments(@PathVariable Long id) {
        List<EmployeeDocumentDTO> documents = employeeDocumentService.getEmployeeDocuments(id);
        ApiResponse<List<EmployeeDocumentDTO>> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Employee documents retrieved successfully",
                documents
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/documents/{docId}/download")
    @Operation(summary = "Download employee document", description = "Download a specific employee document")
    public ResponseEntity<Resource> downloadEmployeeDocument(
            @PathVariable Long id,
            @PathVariable Long docId) {
        try {
            Path filePath = employeeDocumentService.getDocumentFilePath(id, docId);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "application/octet-stream";
            String fileName = filePath.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
