package org.camunda.bpm.getstarted.controller;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.getstarted.entity.HighlightVO;
import org.camunda.bpm.getstarted.service.HighlightService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
public class ProcessController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final HighlightService highlightService;

    @PostMapping("/start")
    public Map<String, Object> startProcess(@RequestParam String assignee) {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("approvalProcess");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();
        for (Task task : tasks) {
            taskService.claim(task.getId(), assignee);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", instance.getId());
        return result;
    }

    @PostMapping("/complete/{taskId}")
    public void completeTask(@PathVariable String taskId, @RequestParam(required = false) Boolean approve) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("approved", approve != null ? approve : true);
        taskService.complete(taskId, vars);
    }

    @GetMapping("/instances")
    public List<Map<String, Object>> listInstances() {
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .orderByProcessInstanceStartTime().desc().list();
        return instances.stream().map(inst -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", inst.getId());
            m.put("businessKey", inst.getBusinessKey());
            m.put("ended", inst.getEndTime() != null);
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/highlight/{instanceId}")
    public HighlightVO getHighlight(@PathVariable String instanceId) {
        return highlightService.getHighlight(instanceId);
    }

    @GetMapping("/todo/{instanceId}")
    public List<Map<String, Object>> getTodoTasks(@PathVariable String instanceId) {
        return taskService.createTaskQuery().processInstanceId(instanceId).list()
                .stream().map(t -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", t.getId());
                    m.put("name", t.getName());
                    m.put("assignee", t.getAssignee());
                    return m;
                }).collect(Collectors.toList());
    }
}
