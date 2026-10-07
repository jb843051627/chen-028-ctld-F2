package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.exception.ctld.CaseFlowRuleException;
import com.fc.v2.mapper.auto.TCtldCaseFlowMapper;
import com.fc.v2.mapper.auto.TCtldCaseFlowTraceMapper;
import com.fc.v2.mapper.auto.TCtldPlotMapper;
import com.fc.v2.mapper.auto.TCtldZoneBookMapper;
import com.fc.v2.model.auto.TCtldCaseFlow;
import com.fc.v2.model.auto.TCtldCaseFlowTrace;
import com.fc.v2.model.auto.TCtldPlot;
import com.fc.v2.model.auto.TCtldZoneBook;
import com.fc.v2.service.ITCtldCaseFlowService;
import com.fc.v2.service.ctld.CaseFlowCreateCmd;
import com.fc.v2.service.ctld.CaseFlowEngine;
import com.fc.v2.service.ctld.CaseFlowEvent;
import com.fc.v2.service.ctld.CaseFlowFacts;
import com.fc.v2.service.ctld.CaseFlowPlan;
import com.fc.v2.service.ctld.CaseFlowReplay;
import com.fc.v2.shiro.util.ShiroUtils;
import com.fc.v2.util.SnowflakeIdWorker;

/**
 * 超标地块纳入受限与解除办件流转单 Service业务层。
 *
 * <p>本类不拿规矩上的主意：档数、放行、按语安置、逆捋、串读法全在 {@link CaseFlowEngine}。
 * 这里只管三件事：把件行锁住（同一件并递两回排成先后）、把库外事实查实（底册挂名、
 * 分区册在册/并走）、把引擎开的单照落库（挪档与留痕同一事务，谁也不许半截）。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TCtldCaseFlowServiceImpl implements ITCtldCaseFlowService {

    private final CaseFlowEngine engine = new CaseFlowEngine();

    /** 幂等码在履历备注栏里的前缀，回放取数不读它，只用来认出重递的那一笔。 */
    private static final String IDEM_PREFIX = "IDEM:";

    @Resource
    private TCtldCaseFlowMapper ctldCaseFlowMapper;

    @Resource
    private TCtldCaseFlowTraceMapper traceMapper;

    @Resource
    private TCtldPlotMapper plotMapper;

    @Resource
    private TCtldZoneBookMapper zoneBookMapper;

    // ---------------------------------------------------------------- 查

    @Override
    public TCtldCaseFlow selectTCtldCaseFlowById(Long id) {
        return this.ctldCaseFlowMapper.selectById(id);
    }

    @Override
    public List<TCtldCaseFlow> selectTCtldCaseFlowList(QueryWrapper<TCtldCaseFlow> queryWrapper) {
        return this.ctldCaseFlowMapper.selectList(queryWrapper);
    }

    // ---------------------------------------------------------------- 起件

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow create(CaseFlowCreateCmd cmd, String idemToken) {
        CaseFlowEngine.validateCreateInput(cmd.getPlotNo(), cmd.getZoneNo(), cmd.getCadValue(), cmd.getMeasureName());
        String actor = currentActor();

        // 一处地块任何一刻只许一条没闭上的件：活件占位键唯一，库面再挡一道。
        Integer dup = this.ctldCaseFlowMapper.selectCount(new QueryWrapper<TCtldCaseFlow>()
                .eq("plot_no", cmd.getPlotNo())
                .eq("status", CaseFlowEngine.STATUS_ACTIVE)
                .eq("del_flag", 0));
        if (dup != null && dup > 0) {
            throw new CaseFlowRuleException("地块「" + cmd.getPlotNo() + "」已有一条没闭上的件，"
                    + "不管前一条还在办还是中途歇下，第二条都起不来；等前一条收口再递");
        }

        TCtldCaseFlow row = new TCtldCaseFlow();
        row.setBizNo(generateBizNo());
        row.setPlotNo(cmd.getPlotNo());
        row.setZoneNo(cmd.getZoneNo());
        row.setCadValue(cmd.getCadValue());
        row.setMeasureName(cmd.getMeasureName());
        row.setApplicant(cmd.getApplicant());
        row.setDelFlag(0);
        row.setOpenKey(cmd.getPlotNo());

        Date now = new Date();
        CaseFlowPlan plan = this.engine.open(cmd.getPlotNo(), cmd.getZoneNo(), cmd.getCadValue(),
                cmd.getMeasureName(), cmd.getApplicant(), actor, now, idemToken);
        applyPlanToRow(row, plan);
        try {
            this.ctldCaseFlowMapper.insert(row);
        } catch (org.springframework.dao.DuplicateKeyException dke) {
            // 同一件并着递两回：活件占位键被先到一笔占了，原样把先到的件还给来人，不另起一件。
            TCtldCaseFlow prior = this.ctldCaseFlowMapper.selectOne(new QueryWrapper<TCtldCaseFlow>()
                    .eq("plot_no", cmd.getPlotNo())
                    .eq("status", CaseFlowEngine.STATUS_ACTIVE)
                    .eq("del_flag", 0)
                    .last("limit 1"));
            if (prior != null) {
                return prior;
            }
            throw dke;
        }
        appendTraces(row, plan, now);
        return row;
    }

    // ---------------------------------------------------------------- 本关留痕

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow note(Long id, String text, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.note(events, text, actor, now, idemToken));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow accept(Long id, String result, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.recordAccept(events, result, actor, now, idemToken));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow witness(Long id, String witnessPost, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.witness(events, witnessPost, actor, now, idemToken));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow cert(Long id, String certNo, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.cert(events, certNo, actor, now, idemToken));
    }

    // ---------------------------------------------------------------- 挪档

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow advance(Long id, String remark) {
        return advance(id, remark, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow advance(Long id, String remark, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.advance(events, loadFacts(row), actor, now, idemToken));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow rollback(Long id, String remark) {
        return rollback(id, remark, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow rollback(Long id, String remark, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.rollback(events, actor, now, idemToken));
    }

    // ---------------------------------------------------------------- 视同收口

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow withdraw(Long id, String reason, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.withdraw(events, row.getApplicant(), actor, reason, now, idemToken));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TCtldCaseFlow zoneMergeClose(Long id, String idemToken) {
        return mutate(id, (row, events, actor, now) ->
                this.engine.zoneMergeClose(events, loadFacts(row), actor, now, idemToken));
    }

    // ---------------------------------------------------------------- 履历与回放

    @Override
    public List<TCtldCaseFlowTrace> traces(Long id) {
        return this.traceMapper.selectList(new QueryWrapper<TCtldCaseFlowTrace>()
                .eq("case_id", id)
                .eq("del_flag", 0)
                .orderByAsc("seq"));
    }

    @Override
    public CaseFlowReplay replay(Long id) {
        TCtldCaseFlow row = this.ctldCaseFlowMapper.selectById(id);
        if (row == null || (row.getDelFlag() != null && row.getDelFlag() == 1)) {
            throw new CaseFlowRuleException("件不存在，无从回放");
        }
        List<CaseFlowEvent> events = loadEvents(id);
        // 只演不写：RowSnapshot 仅作对账，绝不回写。
        return this.engine.replay(events,
                CaseFlowEngine.RowSnapshot.of(row.getStage(), row.getStatus(), row.getContent(), row.getCertNo()));
    }

    @Override
    public CaseFlowEngine.ReconcileResult reconcile(Long id, Integer ledgerStrokes) {
        TCtldCaseFlow row = this.ctldCaseFlowMapper.selectById(id);
        if (row == null || (row.getDelFlag() != null && row.getDelFlag() == 1)) {
            throw new CaseFlowRuleException("件不存在，无从对账");
        }
        List<CaseFlowEvent> events = loadEvents(id);
        return this.engine.reconcile(events, ledgerStrokes == null ? countPositional(events) : ledgerStrokes);
    }

    @Override
    public int countOpen() {
        return this.ctldCaseFlowMapper.countOpen();
    }

    // ---------------------------------------------------------------- 预演（同一处给“放不放行”的答案）

    @Override
    public CaseFlowPlan dryRun(Long id, String action, String payload, String idemToken) {
        TCtldCaseFlow row = this.ctldCaseFlowMapper.selectById(id);
        if (row == null || (row.getDelFlag() != null && row.getDelFlag() == 1)) {
            throw new CaseFlowRuleException("件不存在，无从预演");
        }
        List<CaseFlowEvent> events = loadEvents(id);
        String actor = currentActor();
        Date now = new Date();
        switch (action == null ? "" : action) {
            case "note":
                return this.engine.note(events, payload, actor, now, idemToken);
            case "accept":
                return this.engine.recordAccept(events, payload, actor, now, idemToken);
            case "witness":
                return this.engine.witness(events, payload, actor, now, idemToken);
            case "cert":
                return this.engine.cert(events, payload, actor, now, idemToken);
            case "advance":
                return this.engine.advance(events, loadFacts(row), actor, now, idemToken);
            case "rollback":
                return this.engine.rollback(events, actor, now, idemToken);
            case "withdraw":
                return this.engine.withdraw(events, row.getApplicant(), actor, payload, now, idemToken);
            default:
                throw new CaseFlowRuleException("不认的预演动作：" + action);
        }
    }

    // ---------------------------------------------------------------- 旧口收紧

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateContent(Long id, String remark) {
        TCtldCaseFlow row = this.ctldCaseFlowMapper.selectById(id);
        if (row == null) {
            return false;
        }
        if (row.getStatus() != null && row.getStatus() == CaseFlowEngine.STATUS_TERMINAL) {
            throw new CaseFlowRuleException("件已闭上：更正内容一概不接，名册照露脸、只是不给把手");
        }
        throw new CaseFlowRuleException("在办件不接整栏覆写；要留话走按语口（同一档只留头一句）");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long id) {
        TCtldCaseFlow row = this.ctldCaseFlowMapper.selectById(id);
        if (row == null) {
            return false;
        }
        if (row.getStatus() != null && row.getStatus() == CaseFlowEngine.STATUS_TERMINAL) {
            throw new CaseFlowRuleException("件已闭上：整条抽回一概不接（在办件要收手，由递件人走收回反映口）");
        }
        return this.ctldCaseFlowMapper.deleteById(id) > 0;
    }

    // ---------------------------------------------------------------- 内里把式

    /** 一次件动作的统一壳：锁行→取履历→引擎开单→落主行→追加履历，同一事务。 */
    private TCtldCaseFlow mutate(Long id, MutateAction action) {
        TCtldCaseFlow row = this.ctldCaseFlowMapper.selectByIdForUpdate(id);
        if (row == null) {
            throw new CaseFlowRuleException("件不存在或已删除，动作不受理");
        }
        List<CaseFlowEvent> events = loadEvents(id);
        String actor = currentActor();
        Date now = new Date();
        CaseFlowPlan plan = action.apply(row, events, actor, now);
        if (plan.isIdempotent() || !plan.isChanged()) {
            // 同一件并着递两回：第二回原样认下，不挪档、不另记一笔。
            return row;
        }
        applyPlanToRow(row, plan);
        UpdateWrapper<TCtldCaseFlow> uw = new UpdateWrapper<TCtldCaseFlow>()
                .eq("id", row.getId())
                .eq("del_flag", 0);
        uw.set("stage", row.getStage());
        uw.set("status", row.getStatus());
        uw.set("last_action", row.getLastAction());
        uw.set("update_time", now);
        if (plan.isContentTouched()) {
            uw.set("content", row.getContent());
        }
        if (plan.isCertTouched()) {
            uw.set("cert_no", row.getCertNo());
        }
        if (plan.getCloseKind() != null) {
            uw.set("close_kind", row.getCloseKind());
            // 闭口即腾活件占位键，同一地块可以另起新件。
            uw.set("open_key", null);
        }
        this.ctldCaseFlowMapper.update(null, uw);
        appendTraces(row, plan, now);
        return row;
    }

    private void applyPlanToRow(TCtldCaseFlow row, CaseFlowPlan plan) {
        if (plan.getStage() != null) {
            row.setStage(plan.getStage());
        }
        if (plan.getStatus() != null) {
            row.setStatus(plan.getStatus());
        }
        if (plan.isContentTouched()) {
            row.setContent(plan.getContent());
        }
        if (plan.isCertTouched()) {
            row.setCertNo(plan.getCertNo());
        }
        if (plan.getCloseKind() != null) {
            row.setCloseKind(plan.getCloseKind());
            if (plan.getStatus() != null && plan.getStatus() == CaseFlowEngine.STATUS_TERMINAL) {
                row.setOpenKey(null);
            }
        }
        if (plan.getLastAction() != null) {
            row.setLastAction(plan.getLastAction());
        }
    }

    private void appendTraces(TCtldCaseFlow row, CaseFlowPlan plan, Date now) {
        Integer maxSeq = this.traceMapper.selectMaxSeq(row.getId());
        int floor = maxSeq == null ? 0 : maxSeq;
        for (CaseFlowEvent e : plan.getAppendEvents()) {
            // seq 以库面为底再轧一道：件锁之内，同刻两笔也各占各的先后。
            int seq = Math.max(e.getSeq(), floor + 1);
            floor = seq;
            TCtldCaseFlowTrace t = new TCtldCaseFlowTrace();
            t.setCaseId(row.getId());
            t.setBizNo(row.getBizNo());
            t.setSeq(seq);
            t.setStage(e.getStage());
            t.setRoundNo(e.getRoundNo());
            t.setActionKind(e.getActionKind());
            t.setNote(e.getNote());
            t.setActor(e.getActor());
            t.setWriteTime(e.getWriteTime() == null ? now : e.getWriteTime());
            t.setDelFlag(0);
            if (e.getToken() != null) {
                t.setRemark(IDEM_PREFIX + e.getToken());
            }
            this.traceMapper.insert(t);
        }
    }

    private List<CaseFlowEvent> loadEvents(Long caseId) {
        List<TCtldCaseFlowTrace> rows = this.traceMapper.selectList(new QueryWrapper<TCtldCaseFlowTrace>()
                .eq("case_id", caseId)
                .eq("del_flag", 0)
                .orderByAsc("seq"));
        List<CaseFlowEvent> events = new ArrayList<>();
        for (TCtldCaseFlowTrace t : rows) {
            CaseFlowEvent e = new CaseFlowEvent(t.getSeq(), t.getStage(), t.getRoundNo(),
                    t.getActionKind(), t.getNote(), t.getActor(), t.getWriteTime());
            if (t.getRemark() != null && t.getRemark().startsWith(IDEM_PREFIX)) {
                e.setToken(t.getRemark().substring(IDEM_PREFIX.length()));
            }
            events.add(e);
        }
        return events;
    }

    /** 库外事实：底册挂名、分区册在册/并走。只有这里翻底册，引擎不碰库。 */
    private CaseFlowFacts loadFacts(TCtldCaseFlow row) {
        boolean plotRegistered = false;
        TCtldPlot plot = this.plotMapper.selectOne(new QueryWrapper<TCtldPlot>()
                .eq("plot_no", row.getPlotNo())
                .eq("del_flag", 0)
                .last("limit 1"));
        if (plot != null) {
            plotRegistered = true;
            if (plot.getZoneNo() != null && !plot.getZoneNo().isEmpty()
                    && row.getZoneNo() != null && !plot.getZoneNo().equals(row.getZoneNo())) {
                throw new CaseFlowRuleException("件上分区代号「" + row.getZoneNo()
                        + "」与底册登记的「" + plot.getZoneNo() + "」对不上，先把件上代号改对再挪");
            }
        }
        boolean zoneMerged = false;
        TCtldZoneBook zone = this.zoneBookMapper.selectOne(new QueryWrapper<TCtldZoneBook>()
                .eq("zone_no", row.getZoneNo())
                .eq("del_flag", 0)
                .last("limit 1"));
        if (zone != null && zone.getStatus() != null && zone.getStatus() == 1) {
            zoneMerged = true;
        }
        return CaseFlowFacts.of(plotRegistered, zoneMerged);
    }

    private int countPositional(List<CaseFlowEvent> events) {
        int n = 0;
        for (CaseFlowEvent e : events) {
            if (CaseFlowEvent.OPEN.equals(e.getActionKind())
                    || CaseFlowEvent.ADVANCE.equals(e.getActionKind())
                    || CaseFlowEvent.ROLLBACK.equals(e.getActionKind())) {
                n++;
            }
        }
        return n;
    }

    private String currentActor() {
        try {
            return ShiroUtils.getLoginName();
        } catch (RuntimeException e) {
            // 无登录面（批处理/试演）时不留空名，记“系统”。
            return "系统";
        }
    }

    private String generateBizNo() {
        return "CF" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + SnowflakeIdWorker.getUUID().substring(0, 6).toUpperCase();
    }

    /** mutate 壳与引擎之间的小接缝，免得每个动作各抄一遍锁事。 */
    @FunctionalInterface
    private interface MutateAction {
        CaseFlowPlan apply(TCtldCaseFlow row, List<CaseFlowEvent> events, String actor, Date now);
    }
}
