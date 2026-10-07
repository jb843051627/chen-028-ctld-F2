package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TCtldCaseFlow;
import com.fc.v2.model.auto.TCtldCaseFlowTrace;
import com.fc.v2.service.ctld.CaseFlowCreateCmd;
import com.fc.v2.service.ctld.CaseFlowEngine;
import com.fc.v2.service.ctld.CaseFlowPlan;
import com.fc.v2.service.ctld.CaseFlowReplay;

/**
 * 超标地块纳入受限与解除办件流转单 Service接口。
 * 规矩只认 {@link CaseFlowEngine} 那一处的说法；本接口只做受理、落库、取数。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITCtldCaseFlowService {

    /** 按主键查询单据 */
    TCtldCaseFlow selectTCtldCaseFlowById(Long id);

    /** 列表查询（流转台账；封了口的件照常露脸） */
    List<TCtldCaseFlow> selectTCtldCaseFlowList(QueryWrapper<TCtldCaseFlow> queryWrapper);

    /**
     * 受理起件：四样齐身才起，缺哪样点哪样；一处地块同时只许一条没闭上的件。
     * 返回起好的件。
     */
    TCtldCaseFlow create(CaseFlowCreateCmd cmd, String idemToken);

    /** 本关按语栏留一句话；同档第二句同原话幂等、异原话挡回。 */
    TCtldCaseFlow note(Long id, String text, String idemToken);

    /** 纳入受限档：核收记「可」才可往前；没记成「可」别想开工。result 只认“可/不可”。 */
    TCtldCaseFlow accept(Long id, String result, String idemToken);

    /** 措施核查档：见证岗落一笔（两名不同岗各一笔才算齐）。 */
    TCtldCaseFlow witness(Long id, String witnessPost, String idemToken);

    /** 措施核查档：把措施核验凭证那一串写回本行。 */
    TCtldCaseFlow cert(Long id, String certNo, String idemToken);

    /**
     * 推进一档：一次只跨一档，门槛由引擎逐条点；走满末档即闭上；
     * 分区已并走的按并走视同收口。返回更新后的单据；幂等重递原样返回当下件。
     */
    TCtldCaseFlow advance(Long id, String remark);

    /** 兼容旧口：推进一档（不带投递码）。 */
    TCtldCaseFlow advance(Long id, String remark, String idemToken);

    /**
     * 回退一档：一次只收一档，被收档按语照原样转进追溯串末尾、重来从头累计；
     * 已站头一档没得再收。
     */
    TCtldCaseFlow rollback(Long id, String remark);

    /** 兼容旧口：回退一档（不带投递码）。 */
    TCtldCaseFlow rollback(Long id, String remark, String idemToken);

    /** 递件人本人收回反映，视同收口。 */
    TCtldCaseFlow withdraw(Long id, String reason, String idemToken);

    /** 分区已并走：按分区册事实视同收口（一般由 advance 自动走这一支，也可直接受理）。 */
    TCtldCaseFlow zoneMergeClose(Long id, String idemToken);

    /** 变更履历按时序取全（审计取数来路）。 */
    List<TCtldCaseFlowTrace> traces(Long id);

    /**
     * 回放：从末档一关一关倒着演，只看结果、不写回；
     * 同时与主行当下各栏对账，缺第几档、缺哪一栏逐一点名。
     */
    CaseFlowReplay replay(Long id);

    /**
     * 对账：倒捋档数与受理簿笔数一笔一笔合；ledgerStrokes 传 null 按本件位置笔自核。
     */
    CaseFlowEngine.ReconcileResult reconcile(Long id, Integer ledgerStrokes);

    /** 没闭上那一类的条数，只由这一处查（屏上数与台账同一回给出）。 */
    int countOpen();

    /** 引擎计划预演（不落库）：给屏上/外部问“下一阶放不放行”时，同一处给答案。 */
    CaseFlowPlan dryRun(Long id, String action, String payload, String idemToken);

    /** 修改内容：已封住不得再改；件在办也只许走按语口，不接整栏覆写。被拒返回 false。 */
    boolean updateContent(Long id, String remark);

    /** 删除单据：已封住整条抽回一概不接，被拒返回 false。 */
    boolean remove(Long id);
}
