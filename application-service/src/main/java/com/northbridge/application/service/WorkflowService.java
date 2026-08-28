package com.northbridge.application.service;

import com.northbridge.application.model.ApplicationEvent;

/**
 * Service responsible for starting workflows (Camunda/Zeebe) for application events.
 *
 * Phase 4 will add a real Camunda java client implementation. For now this is a simple
 * abstraction that logs and checks for env configuration.
 */
public interface WorkflowService {

    /**
     * Start a workflow instance for the provided application event.
     * @param event the application event
     */
    void startWorkflow(ApplicationEvent event);
}

