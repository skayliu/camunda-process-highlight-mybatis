package org.camunda.bpm.getstarted.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.camunda.bpm.getstarted.entity.HighlightRecord;

import java.util.List;

@Mapper
public interface HighlightRecordMapper {

    List<HighlightRecord> listByInstanceId(String processInstanceId);

    void insert(HighlightRecord record);

    /**
     * 按主键更新 assignee（最精确，推荐优先使用）
     *
     * @param id       记录主键
     * @param assignee 处理人，传 null 表示清空处理人
     * @return 影响行数，1=更新成功，0=记录不存在
     */
    int updateAssigneeById(@Param("id") Long id,
                           @Param("assignee") String assignee);

    /**
     * 按【流程实例ID + 节点ID】更新最新一条记录的 assignee
     * 适用于任务签收/指派后回填处理人的场景：
     * 任务 start 时 assignee 为 null，签收后调用本方法回填
     *
     * @param processInstanceId 流程实例ID
     * @param elementId         节点ID（BPMN 里的 activityId）
     * @param assignee          处理人
     * @return 影响行数，1=更新成功，0=没有匹配记录
     */
    int updateAssigneeByInstanceAndElement(@Param("processInstanceId") String processInstanceId,
                                           @Param("elementId") String elementId,
                                           @Param("assignee") String assignee);

    /**
     * 按【流程实例ID + 节点ID】更新该节点下所有记录的 assignee
     * 适用于多实例任务、循环节点的批量回填
     *
     * @return 影响行数
     */
    int updateAssigneeByInstanceAndElementAll(@Param("processInstanceId") String processInstanceId,
                                              @Param("elementId") String elementId,
                                              @Param("assignee") String assignee);

    /**
     * 按执行ID更新 assignee
     *
     * @param executionId 执行ID
     * @param assignee    处理人
     * @return 影响行数
     */
    int updateAssigneeByExecutionId(@Param("executionId") String executionId,
                                    @Param("assignee") String assignee);

    /**
     * 动态更新：只更新传入的非 null 字段（assignee / end_time / element_name）
     * 需要更新哪个字段就 set 哪个，其他字段不动
     *
     * @param record 至少包含 id，其他字段为 null 则不更新
     * @return 影响行数
     */
    int updateSelective(HighlightRecord record);

    /**
     * 判断某条记录是否已存在（可选，便于回填前校验）
     */
    int countByInstanceAndElement(@Param("processInstanceId") String processInstanceId,
                                  @Param("elementId") String elementId);
}
