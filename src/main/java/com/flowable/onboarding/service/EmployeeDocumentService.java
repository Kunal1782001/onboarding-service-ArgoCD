package com.flowable.onboarding.service;

import com.flowable.onboarding.dto.EmployeeDocumentDTO;
import com.flowable.onboarding.entity.Employee;
import com.flowable.onboarding.entity.EmployeeDocument;
import com.flowable.onboarding.exception.ResourceNotFoundException;
import com.flowable.onboarding.repository.EmployeeDocumentRepository;
import com.flowable.onboarding.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeDocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    @Value("${app.upload.base-dir:uploads}")
    private String baseUploadDir;

    private final EmployeeRepository employeeRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;

    public Map<String, String> uploadEmployeeDocuments(Long employeeId, Map<String, MultipartFile> documents) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        Path employeeDir = Paths.get(baseUploadDir, "employees", String.valueOf(employee.getId()));
        try {
            Files.createDirectories(employeeDir);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to create upload directory");
        }

        Map<String, String> stored = new LinkedHashMap<>();

        for (Map.Entry<String, MultipartFile> entry : documents.entrySet()) {
            String key = entry.getKey();
            MultipartFile file = entry.getValue();

            validateFile(key, file);

            String extension = getExtension(file.getOriginalFilename());
            String safeName = key + "-" + UUID.randomUUID() + "." + extension;
            Path target = employeeDir.resolve(safeName);

            try {
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                
                // Save document metadata to database
                EmployeeDocument document = EmployeeDocument.builder()
                        .employeeId(employeeId)
                        .documentType(key)
                        .filePath(target.toString())
                        .fileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .build();
                employeeDocumentRepository.save(document);
                
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to store file for " + key);
            }

            stored.put(key, target.toString());
        }

        return stored;
    }

    public List<EmployeeDocumentDTO> getEmployeeDocuments(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        
        List<EmployeeDocument> documents = employeeDocumentRepository.findByEmployeeId(employeeId);
        
        return documents.stream()
                .map(doc -> EmployeeDocumentDTO.builder()
                        .id(doc.getId())
                        .employeeId(doc.getEmployeeId())
                        .documentType(doc.getDocumentType())
                        .fileName(doc.getFileName())
                        .fileSize(doc.getFileSize())
                        .uploadedAt(doc.getUploadedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateFile(String key, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Missing required document: " + key);
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large for " + key + ". Max allowed size is 10 MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file format for " + key + ". Allowed: PDF, JPG, JPEG, PNG");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public Path getDocumentFilePath(Long employeeId, Long documentId) {
        EmployeeDocument document = employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        
        if (!document.getEmployeeId().equals(employeeId)) {
            throw new IllegalArgumentException("Document does not belong to this employee");
        }
        
        return Paths.get(document.getFilePath());
    }
}
