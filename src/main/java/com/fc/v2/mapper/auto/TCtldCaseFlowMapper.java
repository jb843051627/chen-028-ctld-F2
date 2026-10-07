package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TCtldCaseFlow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 超标地块纳入受限与解除办件流转单数据层
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface TCtldCaseFlowMapper extends BaseMapper<TCtldCaseFlow> {

    /**
     * 件行锁：把件这一行锁住，本件“挪档”与“留痕”在同一把锁、同一事务里完成，
     * 同一件并着递两回也排成先后两笔（幂等与否由引擎按原话判定）。
     */
    @Select("SELECT * FROM t_ctld_case_flow WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    TCtldCaseFlow selectByIdForUpdate(@Param("id") Long id);

    /**
     * 没闭上那一类的条数——屏上那一个数与台账查询同一回给出，前端不得另起一套数法。
     */
    @Select("SELECT COUNT(1) FROM t_ctld_case_flow WHERE status = 1 AND del_flag = 0")
    int countOpen();
}
