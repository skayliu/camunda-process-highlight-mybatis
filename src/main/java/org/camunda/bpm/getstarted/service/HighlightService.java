package org.camunda.bpm.getstarted.service;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.getstarted.entity.HighlightRecord;
import org.camunda.bpm.getstarted.entity.HighlightVO;
import org.camunda.bpm.getstarted.mapper.HighlightRecordMapper;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HighlightService {

    private final HighlightRecordMapper recordMapper;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;

    /**
     * 更新/回填处理人（assignee）
     * <p>
     * 典型场景：用户任务 start 事件发生时 assignee 为空（候选组任务未签收），
     * 任务被签收或指派后，调用本方法把处理人回填到高亮记录上。
     * <p>
     * 用 REQUIRES_NEW 开独立事务：本方法常被事件监听器（AFTER_COMMIT / @Async）调用，
     * 独立事务可避免与 Camunda 流程事务、命令上下文相互干扰。
     *
     * @param processInstanceId 流程实例ID
     * @param elementId         节点ID（BPMN activityId）
     * @param assignee          处理人，传 null 表示清空
     * @return 影响行数，1=成功，0=没有匹配记录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateAssignee(String processInstanceId, String elementId, String assignee) {
        if (processInstanceId == null || elementId == null) {
            return 0;
        }
        return recordMapper.updateAssigneeByInstanceAndElement(processInstanceId, elementId, assignee);
    }

    /**
     * 按记录主键更新处理人
     *
     * @param recordId 记录主键
     * @param assignee 处理人
     * @return 影响行数
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int updateAssigneeById(Long recordId, String assignee) {
        if (recordId == null) {
            return 0;
        }
        return recordMapper.updateAssigneeById(recordId, assignee);
    }

    public HighlightVO getHighlight(String processInstanceId) {
        List<HighlightRecord> allRecords = recordMapper.listByInstanceId(processInstanceId);
        HighlightVO vo = new HighlightVO();
        vo.setHistoryList(allRecords);

        if (allRecords.isEmpty()) {
            vo.setCompletedNodes(Collections.emptyList());
            vo.setActiveNodes(Collections.emptyList());
            vo.setHighlightedFlows(Collections.emptyList());
            return vo;
        }

        // 已流转连线：类型为sequenceFlow
        List<String> highlightedFlows = allRecords.stream()
                .filter(r -> "sequenceFlow".equals(r.getElementType()))
                .map(HighlightRecord::getElementId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 已完成节点：有end事件的非连线节点
        Set<String> endedNodes = allRecords.stream()
                .filter(r -> "end".equals(r.getEventType()))
                .map(HighlightRecord::getElementId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 开始节点：有start事件的StartEvent也加入已完成（流程启动后开始事件立即完成）
        allRecords.stream()
                .filter(r -> "start".equals(r.getEventType()))
                .filter(r -> "START_EVENT".equals(r.getElementType()))
                .map(HighlightRecord::getElementId)
                .forEach(endedNodes::add);

        // 已激活过的节点：有start事件的非连线节点
        Set<String> startedNodes = allRecords.stream()
                .filter(r -> "start".equals(r.getEventType()))
                .map(HighlightRecord::getElementId)
                .filter(Objects::nonNull)
                .filter(id -> !"sequenceFlow".equals(id))
                .collect(Collectors.toSet());

        // 活跃节点：start过但还没end
        startedNodes.removeAll(endedNodes);
        List<String> activeNodes = new ArrayList<>(startedNodes);

        vo.setCompletedNodes(new ArrayList<>(endedNodes));
        vo.setActiveNodes(activeNodes);
        vo.setHighlightedFlows(highlightedFlows);

        // 获取BPMN XML
        // 若流程实例已结束，则从历史记录取流程定义Id
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        String processDefinitionId;
        if (instance != null) {
            processDefinitionId = instance.getProcessDefinitionId();
        } else {
            HistoricProcessInstance hi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId).singleResult();
            processDefinitionId = hi.getProcessDefinitionId();
        }
        BpmnModelInstance bpmnModel = repositoryService.getBpmnModelInstance(processDefinitionId);
        vo.setBpmnXml(Bpmn.convertToString(bpmnModel));

        return vo;
    }
}
