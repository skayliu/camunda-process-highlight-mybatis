package org.camunda.bpm.getstarted.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HighlightRecord {
    private Long id;
    private String processInstanceId;
    private String processDefinitionId;
    private String executionId;
    private String elementId;
    private String elementName;
    private String elementType;
    private String eventType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String assignee;
    private LocalDateTime createTime;
}
