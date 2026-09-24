package org.camunda.bpm.getstarted.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.getstarted.entity.HighlightRecord;
import org.camunda.bpm.getstarted.mapper.HighlightRecordMapper;
import org.camunda.bpm.spring.boot.starter.event.ExecutionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HighlightEventListener {

    private final HighlightRecordMapper recordMapper;

    @Async
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    public void onExecutionEvent(ExecutionEvent event) {
        try {
            String eventName = event.getEventName();
            HighlightRecord record = new HighlightRecord();
            record.setProcessInstanceId(event.getProcessInstanceId());
            record.setProcessDefinitionId(event.getProcessDefinitionId());
            // ExecutionEvent 没有 getExecutionId()，执行ID 就是 getId()
            record.setExecutionId(event.getId());
            record.setCreateTime(LocalDateTime.now());
            record.setEventType(eventName);

            if ("take".equals(eventName)) {
                // 连线take事件：取连线ID
                record.setElementId(event.getCurrentTransitionId());
                record.setElementType("sequenceFlow");
                log.info("event------>take---->{}", record.getElementId());
            } else {
                // 节点start/end事件：取节点ID和名称
                record.setElementId(event.getCurrentActivityId());
                record.setElementName(event.getCurrentActivityName());
                if ("start".equals(eventName)) {
                    record.setStartTime(LocalDateTime.now());
                } else if ("end".equals(eventName)) {
                    record.setEndTime(LocalDateTime.now());
                }
                // 获取当前活动类型
                String activityId = event.getCurrentActivityId();
                if (activityId != null && activityId.startsWith("StartEvent")) {
                    record.setElementType("START_EVENT");
                } else if (activityId != null && activityId.startsWith("EndEvent")) {
                    record.setElementType("END_EVENT");
                } else if (activityId != null && activityId.startsWith("Gateway")) {
                    record.setElementType("GATEWAY");
                } else {
                    record.setElementType("TASK");
                }
                // 注意：ExecutionEvent 没有 getAssignee()，处理人由任务事件监听器回填
                log.info("event------>{}---->{}---->{}", eventName, record.getElementId(), record.getElementName());
            }

            // 忽略null值记录
            if (record.getElementId() == null) {
                return;
            }

            recordMapper.insert(record);
        } catch (Exception e) {
            log.error("保存高亮记录失败: {}", e.getMessage(), e);
        }
    }
}
