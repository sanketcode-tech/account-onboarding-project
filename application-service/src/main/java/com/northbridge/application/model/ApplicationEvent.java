package com.northbridge.application.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Application Event Model
 * Represents an application submission event sent to Kafka topic: application.submitted
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationEvent implements Serializable {

    private static final long serialVersionUID = 1L;
private String applicationId;
private String customerId;
private String applicantName;
private String email;
private String status;
private Object payload;
private LocalDateTime timestamp;
private String eventType;

}

