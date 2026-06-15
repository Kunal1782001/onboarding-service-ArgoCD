package com.flowable.onboarding.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.flowable.onboarding.service.ProcessService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplicationStartupConfiguration {

    private final ProcessService processService;

    @EventListener(ApplicationReadyEvent.class)
    public void deployOnStartup() {
        try {
            processService.deployOnboardingProcess();
        } catch (Exception e) {
            System.out.println("Process already deployed or failed to deploy: " + e.getMessage());
        }
    }

}
