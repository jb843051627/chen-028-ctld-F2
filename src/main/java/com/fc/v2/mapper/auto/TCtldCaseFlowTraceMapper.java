package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TCtldCaseFlowTrace;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 超标地块办件变更履历数据层
 *
 * @author fuce
 * @date 2026-10-06
 */
public interface TCtldCaseFlowTraceMapper extends BaseMapper<TCtldCaseFlowTrace> {

    /**
     * 取一件之内当前最大时序笔次；无履历返回 null。
     * 同一件的所有写入在件行锁内进行，seq 只由这一处加一，同刻两笔也各占一笔。
     */
    @Select("SELECT MAX(seq) FROM t_ctld_case_flow_trace WHERE case_id = #{caseId} AND del_flag = 0")
    Integer selectMaxSeq(@Param("caseId") Long caseId);
}
