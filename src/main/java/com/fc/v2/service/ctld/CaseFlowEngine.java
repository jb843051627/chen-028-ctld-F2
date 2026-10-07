package com.fc.v2.service.ctld;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fc.v2.common.exception.ctld.CaseFlowRuleException;

/**
 * 超标地块办件流转的唯一口径处。
 *
 * <p>五问只在这一处给答案，别处不作答、屏上不预设口径：</p>
 * <ol>
 *   <li>件上当下停在第几阶——由履历按时序轧出，报文自带序号只作参考；</li>
 *   <li>下一阶放不放行——门槛在这一处逐条点；</li>
 *   <li>已经合口的行还吞不吞新按语——不吞，三动作一概不接；</li>
 *   <li>同一阶二次投进来的那句话怎么安置——同原话幂等认下，异原话挡回，不留副本；</li>
 *   <li>逆着捋回来该数出几阶——末档驻留档序数，站头一档为 1。</li>
 * </ol>
 * <p>挪档与留痕同出一张 {@link CaseFlowPlan}，Service 只照单落库，不另拿主意。
 * 本类不碰数据库、不看钟表（时刻由调用方传入），好照原样重演、好逐条验。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
public class CaseFlowEngine {

    /** 主行状态：在办 */
    public static final int STATUS_ACTIVE = 1;
    /** 主行状态：已封住 */
    public static final int STATUS_TERMINAL = 2;

    /** 收口来路：走满末一档 */
    public static final int CLOSE_FULL = 0;
    /** 收口来路：递件人收回 */
    public static final int CLOSE_WITHDRAW = 1;
    /** 收口来路：分区并走 */
    public static final int CLOSE_ZONE_MERGE = 2;

    private static final String ACCEPT_OK = "可";

    // ---------------------------------------------------------------- 起件四样

    /**
     * 起一件要四样东西齐身：地块码、所属分区代号、土壤镉实测那一个数、拟采用的措施名目。
     * 缺哪一样点哪一样的名，不许回笼统一句“信息不全”。
     */
    public static void validateCreateInput(String plotNo, String zoneNo, java.math.BigDecimal cadValue, String measureName) {
        List<String> missing = new ArrayList<>();
        if (isBlank(plotNo)) {
            missing.add("地块码");
        }
        if (isBlank(zoneNo)) {
            missing.add("所属分区代号");
        }
        if (cadValue == null) {
            missing.add("土壤镉实测数");
        }
        if (isBlank(measureName)) {
            missing.add("拟采用的措施名目");
        }
        if (!missing.isEmpty()) {
            throw new CaseFlowRuleException("起件四样不齐，缺：" + String.join("、", missing)
                    + "——缺哪样补哪样，不认“信息不全”这种笼统话");
        }
    }

    // ---------------------------------------------------------------- 轧当下

    /** 走过一遍履历，把位置、轮次、各关留痕全部轧成一张快照。 */
    public Snapshot takeSnapshot(List<CaseFlowEvent> events) {
        Snapshot snap = new Snapshot();
        if (events == null || events.isEmpty()) {
            return snap;
        }
        for (CaseFlowEvent e : events) {
            if (snap.tokenSeen.containsKey(e.getToken())) {
                // 库里不该出现：同码两笔。防护性处理，以先到一笔为准。
                continue;
            }
            if (e.getToken() != null) {
                snap.tokenSeen.put(e.getToken(), e.getActionKind());
            }
            int s = e.getStage();
            switch (e.getActionKind()) {
                case CaseFlowEvent.OPEN:
                    snap.stage = CaseFlowStage.ACCEPT_OPEN;
                    snap.rounds[CaseFlowStage.ACCEPT_OPEN] = 1;
                    break;
                case CaseFlowEvent.ADVANCE:
                    snap.stage = s;
                    snap.rounds[s]++;
                    break;
                case CaseFlowEvent.ROLLBACK:
                    // 转抄笔落在哪一档，位置就收到哪一档
                    snap.stage = s;
                    break;
                case CaseFlowEvent.CLOSE:
                    snap.closed = true;
                    snap.closeKind = CLOSE_FULL;
                    break;
                case CaseFlowEvent.WITHDRAW:
                    snap.closed = true;
                    snap.closeKind = CLOSE_WITHDRAW;
                    break;
                case CaseFlowEvent.ZONE_MERGE:
                    snap.closed = true;
                    snap.closeKind = CLOSE_ZONE_MERGE;
                    break;
                default:
                    break;
            }
            snap.records.computeIfAbsent(s, k -> new ArrayList<>());
            if (s >= 0 && s < CaseFlowStage.STAGE_COUNT) {
                snap.records.get(s).add(e);
                if (snap.rounds[s] == 0) {
                    snap.rounds[s] = 1;
                }
            }
        }
        snap.opened = !events.isEmpty();
        return snap;
    }

    /** 当下停在第几阶（0..3）；一件未起为 0（头一档），由履历轧，不信报文序号。 */
    public int currentStage(List<CaseFlowEvent> events) {
        return takeSnapshot(events).stage;
    }

    /** 是否已合口闭上。 */
    public boolean isClosed(List<CaseFlowEvent> events) {
        return takeSnapshot(events).closed;
    }

    // ---------------------------------------------------------------- 起件

    /**
     * 受理起件：四样齐身才起；落 OPEN 一笔，位置钉在头一档。
     */
    public CaseFlowPlan open(String plotNo, String zoneNo, java.math.BigDecimal cadValue, String measureName,
                             String applicant, String actor, Date now, String token) {
        validateCreateInput(plotNo, zoneNo, cadValue, measureName);
        CaseFlowPlan plan = CaseFlowPlan.noop();
        plan.setChanged(true);
        plan.setStage(CaseFlowStage.ACCEPT_OPEN);
        plan.setStatus(STATUS_ACTIVE);
        plan.setContent(null);
        plan.setLastAction(CaseFlowEvent.OPEN);
        plan.getAppendEvents().add(newEvent(1, CaseFlowStage.ACCEPT_OPEN, 1,
                CaseFlowEvent.OPEN, "受理起件：" + measureName, actor, now, token));
        return plan;
    }

    // ---------------------------------------------------------------- 按语

    /**
     * 同一档当前轮次往按语栏塞一句话：
     * 头一句原样留下；第二次同原话并着递来，幂等认下、不动库；
     * 第二次换了一句话，盖不上去也不另存副本，点名挡回。
     */
    public CaseFlowPlan note(List<CaseFlowEvent> events, String text, String actor, Date now, String token) {
        Snapshot snap = guardOpen(events, "添按语");
        if (isBlank(text)) {
            throw new CaseFlowRuleException(CaseFlowStage.labelOf(snap.stage) + "添按语被挡：按语原话是空的，没东西可留");
        }
        if (snap.tokenSeen.containsKey(token)) {
            return CaseFlowPlan.idempotent();
        }
        String first = firstNote(snap, snap.stage, snap.rounds[snap.stage]);
        if (first != null) {
            if (first.equals(text)) {
                return CaseFlowPlan.idempotent();
            }
            throw new CaseFlowRuleException(CaseFlowStage.labelOf(snap.stage)
                    + "按语栏已留过头一句，同档第二句换了说法：既盖不上去、也不另存副本；头一句照留：" + first);
        }
        int seq = nextSeq(events);
        CaseFlowPlan plan = baseTouchPlan(snap, CaseFlowEvent.NOTE);
        plan.setContent(text);
        plan.getAppendEvents().add(newEvent(seq, snap.stage, snap.rounds[snap.stage],
                CaseFlowEvent.NOTE, text, actor, now, token));
        return plan;
    }

    // ---------------------------------------------------------------- 受限核收

    /**
     * 纳入受限那一档的核收记一笔（可/不可）。同轮同结论重记幂等；改口挡回。
     */
    public CaseFlowPlan recordAccept(List<CaseFlowEvent> events, String result, String actor, Date now, String token) {
        Snapshot snap = guardOpen(events, "受限核收");
        if (snap.stage != CaseFlowStage.LIMIT_IN) {
            throw new CaseFlowRuleException("受限核收只能落在" + CaseFlowStage.labelOf(CaseFlowStage.LIMIT_IN)
                    + "，件当下停在" + CaseFlowStage.labelOf(snap.stage));
        }
        String value = ACCEPT_OK.equals(result) ? ACCEPT_OK : "不可";
        if (snap.tokenSeen.containsKey(token)) {
            return CaseFlowPlan.idempotent();
        }
        String prior = acceptResult(snap, snap.rounds[CaseFlowStage.LIMIT_IN]);
        if (prior != null) {
            if (prior.equals(value)) {
                return CaseFlowPlan.idempotent();
            }
            throw new CaseFlowRuleException(CaseFlowStage.labelOf(CaseFlowStage.LIMIT_IN)
                    + "核收已记成「" + prior + "」，同一轮不得改口");
        }
        int seq = nextSeq(events);
        CaseFlowPlan plan = baseTouchPlan(snap, CaseFlowEvent.ACCEPT);
        plan.getAppendEvents().add(newEvent(seq, snap.stage, snap.rounds[snap.stage],
                CaseFlowEvent.ACCEPT, value, actor, now, token));
        return plan;
    }

    // ---------------------------------------------------------------- 见证

    /**
     * 两名见证岗各落一笔：同一岗重笔幂等（不算第二名）；只有两名不同岗各一笔才算齐。
     */
    public CaseFlowPlan witness(List<CaseFlowEvent> events, String witnessPost, String actor, Date now, String token) {
        Snapshot snap = guardOpen(events, "见证落笔");
        if (snap.stage != CaseFlowStage.MEASURE_CHECK) {
            throw new CaseFlowRuleException("见证岗落笔只收在" + CaseFlowStage.labelOf(CaseFlowStage.MEASURE_CHECK)
                    + "，件当下停在" + CaseFlowStage.labelOf(snap.stage));
        }
        if (isBlank(witnessPost)) {
            throw new CaseFlowRuleException(CaseFlowStage.labelOf(CaseFlowStage.MEASURE_CHECK) + "见证落笔被挡：缺见证岗名");
        }
        if (snap.tokenSeen.containsKey(token)) {
            return CaseFlowPlan.idempotent();
        }
        Map<String, CaseFlowEvent> posts = witnessMap(snap, snap.rounds[CaseFlowStage.MEASURE_CHECK]);
        if (posts.containsKey(witnessPost)) {
            return CaseFlowPlan.idempotent();
        }
        int seq = nextSeq(events);
        CaseFlowPlan plan = baseTouchPlan(snap, CaseFlowEvent.WITNESS);
        plan.getAppendEvents().add(newEvent(seq, snap.stage, snap.rounds[snap.stage],
                CaseFlowEvent.WITNESS, witnessPost, actor, now, token));
        return plan;
    }

    // ---------------------------------------------------------------- 凭证

    /**
     * 措施核验凭证那一串写回本行：只在措施核查档收；同串重递幂等，换串照留新一笔（本行写新串）。
     */
    public CaseFlowPlan cert(List<CaseFlowEvent> events, String certNo, String actor, Date now, String token) {
        Snapshot snap = guardOpen(events, "写回措施核验凭证串");
        if (snap.stage != CaseFlowStage.MEASURE_CHECK) {
            throw new CaseFlowRuleException("措施核验凭证串只在" + CaseFlowStage.labelOf(CaseFlowStage.MEASURE_CHECK)
                    + "写回，件当下停在" + CaseFlowStage.labelOf(snap.stage));
        }
        if (isBlank(certNo)) {
            throw new CaseFlowRuleException(CaseFlowStage.labelOf(CaseFlowStage.MEASURE_CHECK) + "凭证写回被挡：缺凭证那一串");
        }
        if (snap.tokenSeen.containsKey(token)) {
            return CaseFlowPlan.idempotent();
        }
        if (certNo.equals(latestCert(snap, snap.rounds[CaseFlowStage.MEASURE_CHECK]))) {
            return CaseFlowPlan.idempotent();
        }
        int seq = nextSeq(events);
        CaseFlowPlan plan = baseTouchPlan(snap, CaseFlowEvent.CERT);
        plan.setCertNo(certNo);
        plan.getAppendEvents().add(newEvent(seq, snap.stage, snap.rounds[snap.stage],
                CaseFlowEvent.CERT, certNo, actor, now, token));
        return plan;
    }

    // ---------------------------------------------------------------- 往前挪

    /**
     * 往前只跨一档，门槛一道一道点；越档递来的不采信、整单留在原地。
     * 到末档即闭上；分区已并走的不往前挪，按并走视同收口。
     */
    public CaseFlowPlan advance(List<CaseFlowEvent> events, CaseFlowFacts facts, String actor, Date now, String token) {
        Snapshot snap = guardOpen(events, "挪档");
        if (snap.tokenSeen.containsKey(token)) {
            return CaseFlowPlan.idempotent();
        }
        int s = snap.stage;
        if (s >= CaseFlowStage.RELEASE_REVIEW) {
            throw new CaseFlowRuleException("件已在末档，无路可前");
        }
        if (facts != null && facts.isZoneMerged()) {
            return closePlan(snap, CaseFlowEvent.ZONE_MERGE, CLOSE_ZONE_MERGE,
                    "所属分区已从分区册并走，视同收口", nextSeq(events), actor, now, token);
        }
        int dest = s + 1;
        // 门槛只认当下这一轮的留痕，不从旁档借旧结论、不照搬别件档数
        if (s == CaseFlowStage.ACCEPT_OPEN) {
            if (facts == null || !facts.isPlotRegistered()) {
                throw new CaseFlowRuleException(CaseFlowStage.labelOf(dest) + "不放行：这一处地块在底册上查不到挂名（已撤出的进不去）");
            }
        } else if (s == CaseFlowStage.LIMIT_IN) {
            if (!ACCEPT_OK.equals(acceptResult(snap, snap.rounds[CaseFlowStage.LIMIT_IN]))) {
                throw new CaseFlowRuleException(CaseFlowStage.labelOf(dest) + "不放行："
                        + CaseFlowStage.labelOf(CaseFlowStage.LIMIT_IN) + "的核收没记成「可」，不许开工");
            }
        } else if (s == CaseFlowStage.MEASURE_CHECK) {
            int round = snap.rounds[CaseFlowStage.MEASURE_CHECK];
            Map<String, CaseFlowEvent> posts = witnessMap(snap, round);
            String certNo = latestCert(snap, round);
            List<String> lack = new ArrayList<>();
            if (posts.size() < 1) {
                lack.add("见证岗第1笔");
            }
            if (posts.size() < 2) {
                lack.add("见证岗第2笔");
            }
            if (isBlank(certNo)) {
                lack.add("措施核验凭证串（写回本行）");
            }
            if (!lack.isEmpty()) {
                throw new CaseFlowRuleException(CaseFlowStage.labelOf(dest) + "不放行，"
                        + CaseFlowStage.labelOf(CaseFlowStage.MEASURE_CHECK) + "还缺：" + String.join("、", lack));
            }
        }

        int roundDest = snap.rounds[dest] + 1;
        int seq = nextSeq(events);
        CaseFlowPlan plan = CaseFlowPlan.noop();
        plan.setChanged(true);
        plan.setStage(dest);
        plan.setStatus(STATUS_ACTIVE);
        plan.setContent(firstNote(snap, dest, roundDest));
        if (dest == CaseFlowStage.MEASURE_CHECK) {
            // 新一轮重新累计，上一轮凭证不顶用
            plan.setCertNo(null);
        }
        plan.setLastAction(CaseFlowEvent.ADVANCE);
        plan.getAppendEvents().add(newEvent(seq, dest, roundDest,
                CaseFlowEvent.ADVANCE, "挪入" + CaseFlowStage.nameOf(dest), actor, now, token));
        if (dest == CaseFlowStage.RELEASE_REVIEW) {
            // 走满末一档即闭上
            plan.setStatus(STATUS_TERMINAL);
            plan.setCloseKind(CLOSE_FULL);
            plan.getAppendEvents().add(newEvent(seq + 1, dest, roundDest,
                    CaseFlowEvent.CLOSE, "走满末档，闭口", actor, now, token));
        }
        return plan;
    }

    // ---------------------------------------------------------------- 往后收

    /**
     * 往后只收一档；被收那一档先前留下的按语照原先后转进追溯串末尾；
     * 重新挪到这一档时从头累计。已站头一档没得再收。
     */
    public CaseFlowPlan rollback(List<CaseFlowEvent> events, String actor, Date now, String token) {
        Snapshot snap = guardOpen(events, "回退");
        if (snap.tokenSeen.containsKey(token)) {
            return CaseFlowPlan.idempotent();
        }
        int s = snap.stage;
        if (s <= CaseFlowStage.ACCEPT_OPEN) {
            throw new CaseFlowRuleException("已站在头一档（初核受理），没得再收");
        }
        int dest = s - 1;
        String oldNote = firstNote(snap, s, snap.rounds[s]);
        int seq = nextSeq(events);
        CaseFlowPlan plan = CaseFlowPlan.noop();
        plan.setChanged(true);
        plan.setStage(dest);
        plan.setStatus(STATUS_ACTIVE);
        plan.setContent(firstNote(snap, dest, snap.rounds[dest]));
        if (s == CaseFlowStage.MEASURE_CHECK) {
            plan.setCertNo(null);
        }
        plan.setLastAction(CaseFlowEvent.ROLLBACK);
        // 转抄笔落在收到的那一档，原话照抄（哪怕原话为空也照空留一笔，次序在末尾）
        plan.getAppendEvents().add(newEvent(seq, dest, snap.rounds[dest],
                CaseFlowEvent.ROLLBACK, oldNote, actor, now, token));
        return plan;
    }

    // ---------------------------------------------------------------- 视同收口

    /** 递件人自己把反映收回去：只有递件本人收才算，视同收口、不再往前挪。 */
    public CaseFlowPlan withdraw(List<CaseFlowEvent> events, String applicant, String operator,
                                 String reason, Date now, String token) {
        Snapshot snap = guardOpen(events, "收回反映");
        if (isBlank(operator) || !operator.equals(applicant)) {
            throw new CaseFlowRuleException("收回反映只认递件那位本人；递件人是「" + applicant + "」，来人是「" + operator + "」");
        }
        return closePlan(snap, CaseFlowEvent.WITHDRAW, CLOSE_WITHDRAW,
                "递件人收回反映" + (isBlank(reason) ? "" : "：" + reason),
                nextSeq(events), operator, now, token);
    }

    /** 所属分区从分区册并走：查实在册已撤才准按此收口。 */
    public CaseFlowPlan zoneMergeClose(List<CaseFlowEvent> events, CaseFlowFacts facts,
                                       String actor, Date now, String token) {
        Snapshot snap = guardOpen(events, "分区并走收口");
        if (facts == null || !facts.isZoneMerged()) {
            throw new CaseFlowRuleException("件所属分区在分区册上仍在册，不能按“并走”收口");
        }
        return closePlan(snap, CaseFlowEvent.ZONE_MERGE, CLOSE_ZONE_MERGE,
                "所属分区已从分区册并走，视同收口",
                nextSeq(events), actor, now, token);
    }

    private CaseFlowPlan closePlan(Snapshot snap, String actionKind, int closeKind,
                                   String note, int seq, String actor, Date now, String token) {
        CaseFlowPlan plan = baseTouchPlan(snap, actionKind);
        plan.setStatus(STATUS_TERMINAL);
        plan.setCloseKind(closeKind);
        plan.getAppendEvents().add(newEvent(seq, snap.stage, snap.rounds[snap.stage],
                actionKind, note, actor, now, token));
        return plan;
    }

    // ---------------------------------------------------------------- 回放（只读）

    /**
     * 把一件从末档一关一关倒着演：只看结果、不写回。
     * 每一步只取该档最后一轮的留痕；缺口点名到第几档、缺哪一栏。
     */
    public CaseFlowReplay replay(List<CaseFlowEvent> events) {
        return replay(events, null);
    }

    /**
     * 倒演并与主行当下各栏对账（仍不写回）。rowSnapshot 可给 null 表示只演不对账。
     */
    public CaseFlowReplay replay(List<CaseFlowEvent> events, RowSnapshot rowSnapshot) {
        Snapshot snap = takeSnapshot(events);
        CaseFlowReplay replay = new CaseFlowReplay();
        if (!snap.opened) {
            replay.setDerivedStage(-1);
            replay.setDerivedStageCount(0);
            replay.getGaps().add("无受理笔（OPEN）：这件还没起，谈不上倒演");
            replay.setConsistent(rowSnapshot == null);
            return replay;
        }
        replay.setDerivedStage(snap.stage);
        replay.setDerivedClosed(snap.closed);
        replay.setDerivedStageCount(snap.stage + 1);

        // 半路收口（递件人收回／分区并走）只闭在当下档，该档不再按正常门槛挑刺
        boolean earlyClose = snap.closed && snap.closeKind != null && snap.closeKind != CLOSE_FULL;
        for (int s = snap.stage; s >= 0; s--) {
            int round = snap.rounds[s];
            List<CaseFlowEvent> roundEvents = eventsOfRound(snap, s, round);
            // 到达笔：头一档是 OPEN，其余是本轮 ADVANCE
            for (CaseFlowEvent e : roundEvents) {
                if ((s == 0 && CaseFlowEvent.OPEN.equals(e.getActionKind()))
                        || (s > 0 && CaseFlowEvent.ADVANCE.equals(e.getActionKind()))) {
                    replay.getSteps().add(toStep(e));
                }
            }
            // 留痕按原时序倒排进步子（倒演：末档在最前，档内后话在前）
            List<CaseFlowEvent> traces = new ArrayList<>();
            for (CaseFlowEvent e : roundEvents) {
                String k = e.getActionKind();
                if (CaseFlowEvent.NOTE.equals(k) || CaseFlowEvent.ACCEPT.equals(k)
                        || CaseFlowEvent.WITNESS.equals(k) || CaseFlowEvent.CERT.equals(k)) {
                    traces.add(e);
                }
            }
            for (int i = traces.size() - 1; i >= 0; i--) {
                replay.getSteps().add(toStep(traces.get(i)));
            }
            // 收到本档的转抄笔
            for (CaseFlowEvent e : roundEvents) {
                if (CaseFlowEvent.ROLLBACK.equals(e.getActionKind())) {
                    replay.getSteps().add(toStep(e));
                }
            }
            // 闭口笔（走满末档）跟在末档；半路收口笔跟在收口那一档
            if ((s == CaseFlowStage.RELEASE_REVIEW && snap.closed) || (earlyClose && s == snap.stage)) {
                for (CaseFlowEvent e : roundEvents) {
                    if (CaseFlowEvent.CLOSE.equals(e.getActionKind())
                            || CaseFlowEvent.WITHDRAW.equals(e.getActionKind())
                            || CaseFlowEvent.ZONE_MERGE.equals(e.getActionKind())) {
                        replay.getSteps().add(toStep(e));
                    }
                }
            }
            if (earlyClose && s == snap.stage) {
                boolean hasEarlyClose = false;
                for (CaseFlowEvent e : roundEvents) {
                    if (CaseFlowEvent.WITHDRAW.equals(e.getActionKind())
                            || CaseFlowEvent.ZONE_MERGE.equals(e.getActionKind())) {
                        hasEarlyClose = true;
                    }
                }
                if (!hasEarlyClose) {
                    replay.getGaps().add(CaseFlowStage.labelOf(s) + "缺：半路收口笔（收回／并走）");
                }
            } else {
                collectGaps(snap, s, round, replay.getGaps());
            }
        }

        if (rowSnapshot != null) {
            List<String> mismatches = new ArrayList<>();
            if (rowSnapshot.getStage() == null || rowSnapshot.getStage() != snap.stage) {
                mismatches.add("当下档数：库存" + rowSnapshot.getStage() + " vs 履历轧出" + snap.stage);
            }
            boolean rowClosed = rowSnapshot.getStatus() != null && rowSnapshot.getStatus() == STATUS_TERMINAL;
            if (rowClosed != snap.closed) {
                mismatches.add("闭口标记：库存" + rowClosed + " vs 履历轧出" + snap.closed);
            }
            String derivedContent = firstNote(snap, snap.stage, snap.rounds[snap.stage]);
            if (!eq(rowSnapshot.getContent(), derivedContent)) {
                mismatches.add("按语栏：库存「" + rowSnapshot.getContent() + "」vs 履历头一句「" + derivedContent + "」");
            }
            String derivedCert = latestCert(snap, snap.rounds[CaseFlowStage.MEASURE_CHECK]);
            if (snap.stage == CaseFlowStage.MEASURE_CHECK && !eq(rowSnapshot.getCertNo(), derivedCert)) {
                mismatches.add("凭证串栏：库存「" + rowSnapshot.getCertNo() + "」vs 履历最新一串「" + derivedCert + "」");
            }
            if (!mismatches.isEmpty()) {
                replay.setConsistent(false);
                replay.getGaps().add("主行与履历对不上：" + String.join("；", mismatches) + "（只演不改，不得据此写回）");
            }
        }
        return replay;
    }

    // ---------------------------------------------------------------- 对账

    /**
     * 倒捋档数与受理簿笔数一笔一笔合不合；单抽任意一档核，自洽不自洽。
     *
     * @param ledgerStrokes 当天受理簿上本件的笔数（每一次受理/挪档/回退各一笔）；
     *                      传 null 表示簿数不在手，按本件位置笔自核
     */
    public ReconcileResult reconcile(List<CaseFlowEvent> events, Integer ledgerStrokes) {
        Snapshot snap = takeSnapshot(events);
        ReconcileResult result = new ReconcileResult();
        if (!snap.opened) {
            result.setMatched(false);
            result.getGaps().add("履历为空：无受理笔");
            return result;
        }
        int positional = 0;
        boolean afterClose = false;
        Integer pos = null;
        for (CaseFlowEvent e : allEventsOrdered(events)) {
            String k = e.getActionKind();
            if (CaseFlowEvent.OPEN.equals(k)) {
                positional++;
                if (pos != null) {
                    result.getGaps().add("出现第二笔受理（seq=" + e.getSeq() + "）：一件只许起一回");
                }
                if (e.getStage() != CaseFlowStage.ACCEPT_OPEN) {
                    result.getGaps().add("受理笔不在头一档（seq=" + e.getSeq() + "）");
                }
                pos = CaseFlowStage.ACCEPT_OPEN;
                if (afterClose) {
                    result.getGaps().add("闭口之后又有受理笔（seq=" + e.getSeq() + "）：合口行不许再动");
                }
            } else if (CaseFlowEvent.ADVANCE.equals(k)) {
                positional++;
                if (pos != null && e.getStage() != pos + 1) {
                    result.getGaps().add("越档笔（seq=" + e.getSeq() + "）：从第" + (pos + 1) + "档一下到了第"
                            + (e.getStage() + 1) + "档，这笔不采信，整条应留在原地");
                }
                if (pos != null) {
                    pos = e.getStage();
                }
                if (afterClose) {
                    result.getGaps().add("闭口之后又有挪档笔（seq=" + e.getSeq() + "）：合口行不许再动");
                }
            } else if (CaseFlowEvent.ROLLBACK.equals(k)) {
                positional++;
                if (pos != null && e.getStage() != pos - 1) {
                    result.getGaps().add("回退笔越档（seq=" + e.getSeq() + "）：只准一档一档收");
                }
                if (pos != null) {
                    pos = e.getStage();
                }
                if (afterClose) {
                    result.getGaps().add("闭口之后又有回退笔（seq=" + e.getSeq() + "）：合口行不许再动");
                }
            }
            if (CaseFlowEvent.CLOSE.equals(k) || CaseFlowEvent.WITHDRAW.equals(k) || CaseFlowEvent.ZONE_MERGE.equals(k)) {
                afterClose = true;
            }
        }
        result.setPositionalStrokes(positional);
        result.setLedgerStrokes(ledgerStrokes == null ? positional : ledgerStrokes);
        result.setDerivedStageCount(snap.stage + 1);
        if (ledgerStrokes != null && positional != ledgerStrokes) {
            result.setMatched(false);
            result.getGaps().add("受理簿笔数对不上：簿上" + ledgerStrokes + "笔，履历里位置笔" + positional + "笔");
        }
        for (int s = 0; s < CaseFlowStage.STAGE_COUNT; s++) {
            for (int round = 1; round <= snap.rounds[s]; round++) {
                List<String> notes = new ArrayList<>();
                for (CaseFlowEvent e : eventsOfRound(snap, s, round)) {
                    if (CaseFlowEvent.NOTE.equals(e.getActionKind())) {
                        if (notes.contains(e.getNote())) {
                            result.getGaps().add(CaseFlowStage.labelOf(s) + "第" + round + "轮：同一句按语存了两本账（seq=" + e.getSeq() + "）");
                        }
                        notes.add(e.getNote());
                    }
                }
                if (notes.size() > 1) {
                    result.getGaps().add(CaseFlowStage.labelOf(s) + "第" + round + "轮：按语栏有两句不同的话，头一句之外的那句不该落账");
                }
            }
        }
        if (!result.getGaps().isEmpty()) {
            result.setMatched(false);
        }
        return result;
    }

    // ---------------------------------------------------------------- 内部小器

    private Snapshot guardOpen(List<CaseFlowEvent> events, String action) {
        Snapshot snap = takeSnapshot(events);
        if (!snap.opened) {
            throw new CaseFlowRuleException(action + "被挡：这件还没受理起件");
        }
        if (snap.closed) {
            throw new CaseFlowRuleException(action + "被挡：件已闭上（收口来路：" + closeName(snap.closeKind)
                    + "），添按语、更正内容、整条抽回三种动作一概不接；名册里照露脸，只是不给把手");
        }
        return snap;
    }

    private String closeName(Integer kind) {
        if (kind == null) {
            return "未收口";
        }
        switch (kind) {
            case CLOSE_FULL:
                return "走满末档";
            case CLOSE_WITHDRAW:
                return "递件人收回";
            case CLOSE_ZONE_MERGE:
                return "分区并走";
            default:
                return "来路不明(" + kind + ")";
        }
    }

    private void collectGaps(Snapshot snap, int stage, int round, List<String> gaps) {
        List<CaseFlowEvent> es = eventsOfRound(snap, stage, round);
        boolean arrived = false;
        for (CaseFlowEvent e : es) {
            if (CaseFlowEvent.OPEN.equals(e.getActionKind()) || CaseFlowEvent.ADVANCE.equals(e.getActionKind())) {
                arrived = true;
            }
        }
        if (!arrived) {
            gaps.add(CaseFlowStage.labelOf(stage) + "缺：到达笔（受理/挪入）");
            return;
        }
        if (stage == CaseFlowStage.LIMIT_IN) {
            if (!ACCEPT_OK.equals(acceptResult(snap, round))) {
                gaps.add(CaseFlowStage.labelOf(stage) + "缺：受限核收「可」这一栏");
            }
        } else if (stage == CaseFlowStage.MEASURE_CHECK) {
            int n = witnessMap(snap, round).size();
            if (n < 2) {
                gaps.add(CaseFlowStage.labelOf(stage) + "缺：见证岗第" + (n + 1) + "笔（须两名不同见证岗各落一笔）");
            }
            if (isBlank(latestCert(snap, round))) {
                gaps.add(CaseFlowStage.labelOf(stage) + "缺：措施核验凭证串（写回本行那一栏）");
            }
        } else if (stage == CaseFlowStage.RELEASE_REVIEW) {
            boolean hasClose = false;
            for (CaseFlowEvent e : es) {
                if (CaseFlowEvent.CLOSE.equals(e.getActionKind())
                        || CaseFlowEvent.WITHDRAW.equals(e.getActionKind())
                        || CaseFlowEvent.ZONE_MERGE.equals(e.getActionKind())) {
                    hasClose = true;
                }
            }
            if (!hasClose) {
                gaps.add(CaseFlowStage.labelOf(stage) + "缺：闭口笔");
            }
        }
    }

    private CaseFlowReplay.Step toStep(CaseFlowEvent e) {
        CaseFlowReplay.Step step = new CaseFlowReplay.Step();
        step.setSeq(e.getSeq());
        step.setStage(e.getStage());
        step.setStageName(CaseFlowStage.nameOf(e.getStage()));
        step.setRoundNo(e.getRoundNo());
        step.setActionKind(e.getActionKind());
        step.setNote(e.getNote());
        step.setActor(e.getActor());
        step.setWriteTime(e.getWriteTime());
        return step;
    }

    private CaseFlowPlan baseTouchPlan(Snapshot snap, String action) {
        CaseFlowPlan plan = CaseFlowPlan.noop();
        plan.setChanged(true);
        plan.setStage(snap.stage);
        plan.setStatus(STATUS_ACTIVE);
        plan.setLastAction(action);
        return plan;
    }

    private CaseFlowEvent newEvent(int seq, int stage, int round, String kind, String note,
                                   String actor, Date now, String token) {
        CaseFlowEvent e = new CaseFlowEvent(seq, stage, round, kind, note, actor, now);
        e.setToken(token);
        return e;
    }

    private int nextSeq(List<CaseFlowEvent> events) {
        int max = 0;
        if (events != null) {
            for (CaseFlowEvent e : events) {
                if (e.getSeq() > max) {
                    max = e.getSeq();
                }
            }
        }
        return max + 1;
    }

    private List<CaseFlowEvent> eventsOfRound(Snapshot snap, int stage, int round) {
        List<CaseFlowEvent> list = new ArrayList<>();
        List<CaseFlowEvent> all = snap.records.get(stage);
        if (all == null) {
            return list;
        }
        for (CaseFlowEvent e : all) {
            if (e.getRoundNo() == round) {
                list.add(e);
            }
        }
        return list;
    }

    private List<CaseFlowEvent> allEventsOrdered(List<CaseFlowEvent> events) {
        List<CaseFlowEvent> copy = new ArrayList<>(events == null ? new ArrayList<>() : events);
        copy.sort((a, b) -> Integer.compare(a.getSeq(), b.getSeq()));
        return copy;
    }

    private String firstNote(Snapshot snap, int stage, int round) {
        for (CaseFlowEvent e : eventsOfRound(snap, stage, round)) {
            if (CaseFlowEvent.NOTE.equals(e.getActionKind())) {
                return e.getNote();
            }
        }
        return null;
    }

    private String acceptResult(Snapshot snap, int round) {
        for (CaseFlowEvent e : eventsOfRound(snap, CaseFlowStage.LIMIT_IN, round)) {
            if (CaseFlowEvent.ACCEPT.equals(e.getActionKind())) {
                return e.getNote();
            }
        }
        return null;
    }

    private Map<String, CaseFlowEvent> witnessMap(Snapshot snap, int round) {
        Map<String, CaseFlowEvent> map = new LinkedHashMap<>();
        for (CaseFlowEvent e : eventsOfRound(snap, CaseFlowStage.MEASURE_CHECK, round)) {
            if (CaseFlowEvent.WITNESS.equals(e.getActionKind())) {
                map.putIfAbsent(e.getNote(), e);
            }
        }
        return map;
    }

    private String latestCert(Snapshot snap, int round) {
        String latest = null;
        for (CaseFlowEvent e : eventsOfRound(snap, CaseFlowStage.MEASURE_CHECK, round)) {
            if (CaseFlowEvent.CERT.equals(e.getActionKind())) {
                latest = e.getNote();
            }
        }
        return latest;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean eq(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /** 履历轧出来的内部快照。 */
    public static class Snapshot {
        private boolean opened;
        private int stage;
        private boolean closed;
        private Integer closeKind;
        private final int[] rounds = new int[CaseFlowStage.STAGE_COUNT];
        private final Map<Integer, List<CaseFlowEvent>> records = new LinkedHashMap<>();
        private final Map<String, String> tokenSeen = new LinkedHashMap<>();

        public int getStage() {
            return stage;
        }

        public boolean isClosed() {
            return closed;
        }

        public int[] getRounds() {
            return rounds;
        }
    }

    /** 对账结果。 */
    public static class ReconcileResult {
        private boolean matched = true;
        private int positionalStrokes;
        private int ledgerStrokes;
        private int derivedStageCount;
        private final List<String> gaps = new ArrayList<>();

        public boolean isMatched() {
            return matched;
        }

        public void setMatched(boolean matched) {
            this.matched = matched;
        }

        public int getPositionalStrokes() {
            return positionalStrokes;
        }

        public void setPositionalStrokes(int positionalStrokes) {
            this.positionalStrokes = positionalStrokes;
        }

        public int getLedgerStrokes() {
            return ledgerStrokes;
        }

        public void setLedgerStrokes(int ledgerStrokes) {
            this.ledgerStrokes = ledgerStrokes;
        }

        public int getDerivedStageCount() {
            return derivedStageCount;
        }

        public void setDerivedStageCount(int derivedStageCount) {
            this.derivedStageCount = derivedStageCount;
        }

        public List<String> getGaps() {
            return gaps;
        }
    }

    /** 对账时主行只读快照。 */
    public static class RowSnapshot {
        private Integer stage;
        private Integer status;
        private String content;
        private String certNo;

        public static RowSnapshot of(Integer stage, Integer status, String content, String certNo) {
            RowSnapshot r = new RowSnapshot();
            r.stage = stage;
            r.status = status;
            r.content = content;
            r.certNo = certNo;
            return r;
        }

        public Integer getStage() {
            return stage;
        }

        public Integer getStatus() {
            return status;
        }

        public String getContent() {
            return content;
        }

        public String getCertNo() {
            return certNo;
        }
    }
}
