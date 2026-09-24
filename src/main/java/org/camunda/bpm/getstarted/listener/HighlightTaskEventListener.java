package org.camunda.bpm.getstarted.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.getstarted.mapper.HighlightRecordMapper;
import org.camunda.bpm.spring.boot.starter.event.TaskEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 任务事件监听器：负责把处理人（assignee）回填到高亮记录上
 * <p>
 * 为什么单独用一个监听器？
 * 流程流转用的 ExecutionEvent 里没有 assignee 字段，
 * 处理人只在 TaskEvent（任务事件）里才有，所以需要分开监听。
 * <p>
 * 前提：application.yml 中需开启 camunda.bpm.eventing.task: true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HighlightTaskEventListener {

    private final HighlightRecordMapper recordMapper;

    /**
     * 监听任务事件，回填 assignee
     * <p>
     * eventName 取值：create（任务创建）、assignment（签收/指派）、
     * complete（完成）、delete（删除）
     * <p>
     * 任务被签收、指派时 eventName = assignment，此时 assignee 才有值，
     * 调用 updateAssignee 把处理人更新到该节点已有的高亮记录上。
     */
    @Async
    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT,
        fallbackExecution = true
    )
    public void onTaskEvent(TaskEvent event) {
        try {
            String assignee = event.getAssignee();
            // 没有处理人（未签收、或事件里不带处理人）就不用更新
            if (assignee == null || assignee.isEmpty()) {
                return;
            }

            String processInstanceId = event.getProcessInstanceId();
            // 任务定义Key 就是 BPMN 里该节点的 ID，和高亮记录里的 element_id 一致
            String elementId = event.getTaskDefinitionKey();
            if (processInstanceId == null || elementId == null) {
                return;
            }

            int rows = recordMapper.updateAssigneeByInstanceAndElement(processInstanceId, elementId, assignee);
            log.info("回填 assignee -> 实例={} 节点={} 处理人={} 影响行数={}",
                    processInstanceId, elementId, assignee, rows);

        } catch (Exception e) {
            // 异常自己吞掉，不能影响主流程
            log.error("回填 assignee 失败: {}", e.getMessage(), e);
        }
    }
}
