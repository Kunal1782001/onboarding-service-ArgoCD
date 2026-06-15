package com.flowable.onboarding.service;

import com.flowable.onboarding.dto.EmployeeDTO;
import com.flowable.onboarding.entity.Employee;
import com.flowable.onboarding.exception.ProcessException;
import com.flowable.onboarding.exception.ResourceNotFoundException;
import com.flowable.onboarding.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProcessService {

    private final ProcessEngine processEngine;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final EmployeeRepository employeeRepository;
    private final TaskService taskService;
    private final ProcessAuditService auditService;
    private final ModelMapper modelMapper;

    public static final String PROCESS_KEY = "employeeOnboarding";
    public static final String PROCESS_DEFINITION_PATH = "classpath:bpmn/employee-onboarding.bpmn20.xml";

    /**
     * Deploy the onboarding process definition
     */
    public void deployOnboardingProcess() {
        try {
            log.info("Deploying onboarding process...");

            // Read the BPMN file
            InputStream bpmnStream = this.getClass().getResourceAsStream("/bpmn/employee-onboarding.bpmn20.xml");
            if (bpmnStream == null) {
                throw new ProcessException("BPMN file not found: /bpmn/employee-onboarding.bpmn20.xml");
            }

            // Deploy with enableDuplicateFiltering — only creates new version if content changed
      repositoryService.createDeployment()
    .name("Employee Onboarding Process")   // ✅ REQUIRED
    .addInputStream("employee-onboarding.bpmn20.xml", bpmnStream)
    .enableDuplicateFiltering()
    .deploy();

            ProcessDefinition latest = getLatestProcessDefinition();
            log.info("Onboarding process deployed. Version: " + (latest != null ? latest.getVersion() : "unknown"));
        } catch (Exception e) {
            log.error("Error deploying onboarding process", e);
            throw new ProcessException("Failed to deploy onboarding process: " + e.getMessage());
        }
    }

    /**
     * Start a new onboarding process for an employee
     */
    public ProcessInstance startOnboardingProcess(Long employeeId, Map<String, Object> variables) {
        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

            // Prepare variables for process
            Map<String, Object> processVariables = new HashMap<>(variables);
            processVariables.put("employeeId", employeeId);
            processVariables.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
            processVariables.put("employeeEmail", employee.getEmail());
            processVariables.put("managerId", employee.getManagerId());

            // Start the process
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY, processVariables);

            log.info("Onboarding process started for employee: " + employeeId + ", Process ID: " + processInstance.getId());

            // Update employee with process instance ID
            employee.setProcessInstanceId(processInstance.getId());
            employee.setStatus(Employee.OnboardingStatus.IN_PROGRESS);
            employeeRepository.save(employee);

            // Log the action
            auditService.logAction(processInstance.getId(), employeeId, "START", "Process Started", "SYSTEM", 
                    "Onboarding process initiated for " + employee.getFirstName() + " " + employee.getLastName());

            return processInstance;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error starting onboarding process for employee: " + employeeId, e);
            throw new ProcessException("Failed to start onboarding process: " + e.getMessage());
        }
    }

    /**
     * Get process instance details
     */
    public ProcessInstance getProcessInstance(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            throw new ResourceNotFoundException("Process instance not found with id: " + processInstanceId);
        }

        return processInstance;
    }

    /**
     * Get all active process instances
     */
    public List<ProcessInstance> getAllActiveProcesses() {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_KEY)
                .list();
    }

    /**
     * Get process variables
     */
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * Update process variable
     */
    public void setProcessVariable(String processInstanceId, String variableName, Object variableValue) {
        runtimeService.setVariable(processInstanceId, variableName, variableValue);
        log.info("Process variable updated: " + variableName + " = " + variableValue);
    }

    /**
     * Complete a process instance (Administrative)
     */
    public void completeProcessInstance(String processInstanceId, String completedBy, String remarks) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);

        // Get related employee for audit logging
        Employee employee = employeeRepository.findByProcessInstanceId(processInstanceId)
                .orElse(null);

        // Delete the process instance
        runtimeService.deleteProcessInstance(processInstanceId, "Process completed by: " + completedBy);

        if (employee != null) {
            employee.setStatus(Employee.OnboardingStatus.COMPLETED);
            employeeRepository.save(employee);

            auditService.logAction(processInstanceId, employee.getId(), "COMPLETE", "Process Completed", 
                    completedBy, remarks);
        }

        log.info("Process instance completed: " + processInstanceId);
    }

    /**
     * Reject an onboarding process
     */
    public void rejectOnboardingProcess(String processInstanceId, Long employeeId, String reason) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        runtimeService.deleteProcessInstance(processInstanceId, "Rejected: " + reason);

        employee.setStatus(Employee.OnboardingStatus.REJECTED);
        employeeRepository.save(employee);

        auditService.logAction(processInstanceId, employeeId, "REJECTION", "Application Rejected", "SYSTEM", reason);

        log.info("Onboarding process rejected for employee: " + employeeId);
    }

    /**
     * Get process definition details
     */
    public ProcessDefinition getProcessDefinition(String processDefinitionId) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
    }

    /**
     * Get latest process definition
     */
    public ProcessDefinition getLatestProcessDefinition() {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(PROCESS_KEY)
                .orderByProcessDefinitionVersion().desc()
                .list()
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Get process instance status
     */
    public String getProcessInstanceStatus(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        return processInstance.isEnded() ? "COMPLETED" : "IN_PROGRESS";
    }

    /**
     * Generate a process diagram PNG with multi-color state highlighting:
     *   - BLUE  = completed stages & flows
     *   - GREEN = active / in-progress stages & flows
     *   - Default = pending stages
     *
     * Uses Flowable's own renderer (which draws red highlights), then pixel-recolors
     * the red to blue or green so the overlays are pixel-perfect aligned.
     */
    public byte[] generateProcessDiagram(String processInstanceId) {
        try {
            // ---------- 1. Gather runtime & history data ----------
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (processInstance == null) {
                throw new ResourceNotFoundException("Process instance not found: " + processInstanceId);
            }

            ProcessDefinition latestDef = getLatestProcessDefinition();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(latestDef.getId());

            List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
            Set<String> completedActivityIds = getCompletedActivityIds(processInstanceId);
            List<String> completedFlowIds = getHighLightedFlows(processInstanceId);
            List<String> activeFlowIds = getActiveFlows(bpmnModel, completedActivityIds, activeActivityIds);

            // ---------- 2. Generate three diagrams via Flowable ----------
            DefaultProcessDiagramGenerator gen = new DefaultProcessDiagramGenerator();

            // Base (no highlights)
            BufferedImage baseImg = ImageIO.read(gen.generateDiagram(
                    bpmnModel, "png",
                    Collections.emptyList(), Collections.emptyList(),
                    "Arial", "Arial", "Arial", null, 1.0, true));

            // All highlighted (completed + active) in red
            List<String> allActivities = new ArrayList<>(completedActivityIds);
            allActivities.addAll(activeActivityIds);
            List<String> allFlows = new ArrayList<>(completedFlowIds);
            allFlows.addAll(activeFlowIds);
            BufferedImage allRedImg = ImageIO.read(gen.generateDiagram(
                    bpmnModel, "png",
                    allActivities, allFlows,
                    "Arial", "Arial", "Arial", null, 1.0, true));

            // Only active highlighted in red
            BufferedImage activeRedImg = ImageIO.read(gen.generateDiagram(
                    bpmnModel, "png",
                    new ArrayList<>(activeActivityIds), activeFlowIds,
                    "Arial", "Arial", "Arial", null, 1.0, true));

            // ---------- 3. Recolor: red→blue for completed, red→green for active ----------
            int w = baseImg.getWidth();
            int h = baseImg.getHeight();
            int legendHeight = 50;
            BufferedImage result = new BufferedImage(w, h + legendHeight, BufferedImage.TYPE_INT_ARGB);

            // Copy base into result
            int[] basePixels  = baseImg.getRGB(0, 0, w, h, null, 0, w);
            int[] allPx       = allRedImg.getRGB(0, 0, w, h, null, 0, w);
            int[] activePx    = activeRedImg.getRGB(0, 0, w, h, null, 0, w);
            int[] resultPixels = new int[w * (h + legendHeight)];

            // White fill for legend area
            int white = 0xFFFFFFFF;
            for (int i = w * h; i < resultPixels.length; i++) {
                resultPixels[i] = white;
            }

            for (int i = 0; i < w * h; i++) {
                int basePx = basePixels[i];
                int allP   = allPx[i];
                int actP   = activePx[i];

                boolean changedInAll    = (allP != basePx);
                boolean changedInActive = (actP != basePx);

                if (changedInAll && isReddish(allP)) {
                    if (changedInActive && isReddish(actP)) {
                        // Active → recolor to green
                        resultPixels[i] = recolorRedToGreen(allP);
                    } else {
                        // Completed → recolor to blue
                        resultPixels[i] = recolorRedToBlue(allP);
                    }
                } else {
                    resultPixels[i] = basePx;
                }
            }

            result.setRGB(0, 0, w, h + legendHeight, resultPixels, 0, w);

            // ---------- 4. Draw legend ----------
            Graphics2D g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            int ly = h + 10;
            g.setFont(new Font("Arial", Font.BOLD, 13));

            g.setColor(new Color(33, 150, 243));
            g.fillRoundRect(20, ly, 22, 22, 4, 4);
            g.setColor(new Color(50, 50, 50));
            g.drawString("Completed", 50, ly + 16);

            g.setColor(new Color(76, 175, 80));
            g.fillRoundRect(170, ly, 22, 22, 4, 4);
            g.setColor(new Color(50, 50, 50));
            g.drawString("In Progress", 200, ly + 16);

            g.setColor(new Color(189, 189, 189));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(360, ly, 22, 22, 4, 4);
            g.setColor(new Color(50, 50, 50));
            g.drawString("Pending", 390, ly + 16);

            g.dispose();

            // ---------- 5. Encode ----------
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(result, "png", baos);
            log.info("Generated multi-colour process diagram for: {}", processInstanceId);
            return baos.toByteArray();

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating process diagram for: " + processInstanceId, e);
            throw new ProcessException("Failed to generate diagram: " + e.getMessage());
        }
    }

    /**
     * Check if a pixel is significantly red (R channel dominates).
     */
    private boolean isReddish(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8)  & 0xFF;
        int b = argb & 0xFF;
        return r > 120 && r > g + 40 && r > b + 40;
    }

    /**
     * Recolor a red-ish pixel to blue, preserving brightness/alpha.
     */
    private int recolorRedToBlue(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8)  & 0xFF;
        int b = argb & 0xFF;
        // Move the red intensity into blue channel, add some green for a nice blue
        int newR = Math.min(g, 80);
        int newG = Math.min(100, g + 50);
        int newB = Math.min(255, r + 30);
        return (a << 24) | (newR << 16) | (newG << 8) | newB;
    }

    /**
     * Recolor a red-ish pixel to green, preserving brightness/alpha.
     */
    private int recolorRedToGreen(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8)  & 0xFF;
        int b = argb & 0xFF;
        // Move the red intensity into green channel
        int newR = Math.min(b, 80);
        int newG = Math.min(255, r + 20);
        int newB = Math.min(b, 80);
        return (a << 24) | (newR << 16) | (newG << 8) | newB;
    }

    /**
     * Get IDs of activities that have been completed (endTime != null).
     */
    private Set<String> getCompletedActivityIds(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .list()
                .stream()
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toSet());
    }

    /**
     * Get sequence flow IDs leading into currently active activities.
     */
    private List<String> getActiveFlows(BpmnModel model, Set<String> completedIds,
                                        List<String> activeIds) {
        List<String> result = new ArrayList<>();
        for (FlowElement fe : model.getMainProcess().getFlowElements()) {
            if (fe instanceof SequenceFlow sf) {
                // A flow is "active" if its source is completed and its target is currently active
                if ((completedIds.contains(sf.getSourceRef()) || sf.getSourceRef().contains("Gateway"))
                        && activeIds.contains(sf.getTargetRef())) {
                    result.add(sf.getId());
                }
            }
        }
        return result;
    }

    /**
     * Generate diagram for the process definition (no active highlights)
     */
    public byte[] generateProcessDefinitionDiagram() {
        try {
            ProcessDefinition processDefinition = getLatestProcessDefinition();
            if (processDefinition == null) {
                throw new ResourceNotFoundException("No process definition found for key: " + PROCESS_KEY);
            }

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());

            DefaultProcessDiagramGenerator diagramGenerator = new DefaultProcessDiagramGenerator();
            InputStream diagramStream = diagramGenerator.generateDiagram(
                    bpmnModel, "png",
                    Collections.emptyList(), Collections.emptyList(),
                    "Arial", "Arial", "Arial",
                    null, 1.0, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = diagramStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            diagramStream.close();
            return baos.toByteArray();

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating process definition diagram", e);
            throw new ProcessException("Failed to generate diagram: " + e.getMessage());
        }
    }

    /**
     * Determine which sequence flows have been fully traversed (both ends completed).
     */
    private List<String> getHighLightedFlows(String processInstanceId) {
        List<String> flowIds = new ArrayList<>();
        try {
            Set<String> completedIds = getCompletedActivityIds(processInstanceId);

            ProcessDefinition latestDef = getLatestProcessDefinition();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(latestDef.getId());

            for (FlowElement fe : bpmnModel.getMainProcess().getFlowElements()) {
                if (fe instanceof SequenceFlow sf) {
                    if (completedIds.contains(sf.getSourceRef())
                            && completedIds.contains(sf.getTargetRef())) {
                        flowIds.add(sf.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine highlighted flows: {}", e.getMessage());
        }
        return flowIds;
    }

}
