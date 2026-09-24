package org.camunda.bpm.getstarted.entity;

import lombok.Data;
import java.util.List;

@Data
public class HighlightVO {
    private List<String> completedNodes;
    private List<String> activeNodes;
    private List<String> highlightedFlows;
    private String bpmnXml;
    private List<HighlightRecord> historyList;
}
