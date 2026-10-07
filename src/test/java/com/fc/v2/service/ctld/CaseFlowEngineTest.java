package com.fc.v2.service.ctld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fc.v2.common.exception.ctld.CaseFlowRuleException;

/**
 * 五问五件事的纯内存钉验：不启 Spring、不碰库。
 * 内存壳 {@code MemCase} 模仿 Service 把引擎的单照落到一串履历上，
 * 顺便钉“挪档与留痕同出一张单”。
 *
 * @author fuce
 * @date 2026-10-06
 */
class CaseFlowEngineTest {

    private final CaseFlowEngine engine = new CaseFlowEngine();
    private static final Date T0 = new Date(946684800000L); // 固定时刻，同刻两笔好造

    /** 内存件：一行主栏 + 一串履历。 */
    class MemCase {
        List<CaseFlowEvent> events = new ArrayList<>();
        String content;
        String certNo;
        Integer stage;
        Integer status;
        Integer closeKind;
        String applicant = "张三";

        void apply(CaseFlowPlan p) {
            if (p.isIdempotent() || !p.isChanged()) {
                return;
            }
            if (p.getStage() != null) {
                stage = p.getStage();
            }
            if (p.getStatus() != null) {
                status = p.getStatus();
            }
            if (p.isContentTouched()) {
                content = p.getContent();
            }
            if (p.isCertTouched()) {
                certNo = p.getCertNo();
            }
            if (p.getCloseKind() != null) {
                closeKind = p.getCloseKind();
            }
            events.addAll(p.getAppendEvents());
        }
    }

    private MemCase openCase() {
        MemCase c = new MemCase();
        c.apply(engine.open("DK-1", "QC00", new BigDecimal("2.80"), "深翻耕+种植结构调整", "张三", "李四", T0, null));
        return c;
    }

    private CaseFlowFacts facts(boolean plot, boolean merged) {
        return CaseFlowFacts.of(plot, merged);
    }

    private long countKind(List<CaseFlowEvent> es, String kind) {
        return es.stream().filter(e -> e.getActionKind().equals(kind)).count();
    }

    // ================================================================ 其一：起件与档数

    @Test
    void create_lacksEachField_messageNamesIt() {
        assertMissing(null, "QC00", new BigDecimal("2.8"), "措施", "地块码");
        assertMissing("DK-1", " ", new BigDecimal("2.8"), "措施", "所属分区代号");
        assertMissing("DK-1", "QC00", null, "措施", "土壤镉实测数");
        assertMissing("DK-1", "QC00", new BigDecimal("2.8"), "", "拟采用的措施名目");
    }

    private void assertMissing(String plot, String zone, BigDecimal cad, String measure, String named) {
        CaseFlowRuleException ex = assertThrows(CaseFlowRuleException.class,
                () -> engine.validateCreateInput(plot, zone, cad, measure));
        assertTrue(ex.getMessage().contains(named), "报错得点出缺的是「" + named + "」，原话：" + ex.getMessage());
        assertFalse(ex.getMessage().trim().equals("信息不全"));
    }

    @Test
    void create_lacksSeveral_listsAllNames() {
        CaseFlowRuleException ex = assertThrows(CaseFlowRuleException.class,
                () -> engine.validateCreateInput(null, null, null, "措施"));
        assertTrue(ex.getMessage().contains("地块码"));
        assertTrue(ex.getMessage().contains("所属分区代号"));
        assertTrue(ex.getMessage().contains("土壤镉实测数"));
    }

    @Test
    void stageCount_isFour_andOpenSitsAtStageZero() {
        assertEquals(4, CaseFlowStage.STAGE_COUNT);
        MemCase c = openCase();
        assertEquals(0, engine.currentStage(c.events));
        // 档数由履历数出来：履历里只有一笔 OPEN
        assertEquals(1, c.events.size());
    }

    /** 报文自带序号一概不认：引擎压根不收“报来的档数”，档数只从履历轧。 */
    @Test
    void stageNumber_derivedFromTrace_notFromReport() {
        MemCase c = new MemCase();
        // 手工摆一串履历：受理 + 两次挪入（等于走到措施核查）
        c.events.add(new CaseFlowEvent(1, 0, 1, CaseFlowEvent.OPEN, "受理", "李四", T0));
        c.events.add(new CaseFlowEvent(2, 1, 1, CaseFlowEvent.ADVANCE, "挪入", "李四", T0));
        c.events.add(new CaseFlowEvent(3, 2, 1, CaseFlowEvent.ADVANCE, "挪入", "李四", T0));
        assertEquals(2, engine.currentStage(c.events));
    }

    // ================================================================ 其二：门槛、一次一档、收口

    @Test
    void advance_onlyOneStepAtATime_regressionOldPlusTwoBug() {
        MemCase c = openCase();
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        // 旧码是 st+2，钉死它只走一档
        assertEquals(1, engine.currentStage(c.events));
        c.apply(engine.recordAccept(c.events, "可", "王五", T0, null));
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertEquals(2, engine.currentStage(c.events));
    }

    @Test
    void advance_toLimit_requiresPlotRegistered() {
        MemCase c = openCase();
        CaseFlowRuleException ex = assertThrows(CaseFlowRuleException.class,
                () -> engine.advance(c.events, facts(false, false), "李四", T0, null));
        assertTrue(ex.getMessage().contains("底册"));
        assertEquals(0, engine.currentStage(c.events), "不采信的越件，整条留在原地");
    }

    @Test
    void advance_toMeasure_requiresAcceptOk() {
        MemCase c = openCase();
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        CaseFlowRuleException ex = assertThrows(CaseFlowRuleException.class,
                () -> engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertTrue(ex.getMessage().contains("核收"));
        assertTrue(ex.getMessage().contains("可"));
        // 记了“不可”也不行
        c.apply(engine.recordAccept(c.events, "不可", "王五", T0, null));
        assertThrows(CaseFlowRuleException.class,
                () -> engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertEquals(1, engine.currentStage(c.events));
    }

    @Test
    void advance_toRelease_requiresTwoWitnessPostsAndCert_namesEachMissing() {
        MemCase c = atMeasureStage();
        // 一样没给：三样缺名都得点到
        CaseFlowRuleException ex = assertThrows(CaseFlowRuleException.class,
                () -> engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertTrue(ex.getMessage().contains("见证岗第1笔"));
        assertTrue(ex.getMessage().contains("见证岗第2笔"));
        assertTrue(ex.getMessage().contains("措施核验凭证串"));

        c.apply(engine.witness(c.events, "村监管岗甲", "赵六", T0, null));
        c.apply(engine.witness(c.events, "镇复核岗乙", "钱七", T0, null));
        // 还差凭证
        CaseFlowRuleException ex2 = assertThrows(CaseFlowRuleException.class,
                () -> engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertFalse(ex2.getMessage().contains("见证岗"));
        assertTrue(ex2.getMessage().contains("措施核验凭证串"));

        c.apply(engine.cert(c.events, "CERT-2026-0001", "孙八", T0, null));
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertEquals(3, engine.currentStage(c.events));
        assertTrue(engine.isClosed(c.events), "走满末档即闭上");
        assertEquals(CaseFlowEngine.CLOSE_FULL, c.closeKind);
    }

    @Test
    void witness_samePostTwice_isIdempotent_notCountedAsSecond() {
        MemCase c = atMeasureStage();
        c.apply(engine.witness(c.events, "村监管岗甲", "赵六", T0, "tok-a"));
        CaseFlowPlan again = engine.witness(c.events, "村监管岗甲", "赵六", T0, "tok-b");
        assertTrue(again.isIdempotent());
        assertEquals(1, countKind(c.events, CaseFlowEvent.WITNESS));
    }

    @Test
    void zoneMerged_advance_closesInsteadOfMoving() {
        MemCase c = openCase();
        c.apply(engine.advance(c.events, facts(true, true), "李四", T0, null));
        assertTrue(engine.isClosed(c.events));
        assertEquals(0, engine.currentStage(c.events), "不往前挪，停在原地收口");
        assertEquals(CaseFlowEngine.CLOSE_ZONE_MERGE, c.closeKind);
    }

    @Test
    void withdraw_onlyApplicantCloses() {
        MemCase c = atMeasureStage();
        assertThrows(CaseFlowRuleException.class,
                () -> engine.withdraw(c.events, c.applicant, "李四（经办人）", "不想搞了", T0, null));
        c.apply(engine.withdraw(c.events, c.applicant, "张三", "本人申请撤回", T0, null));
        assertTrue(engine.isClosed(c.events));
        assertEquals(CaseFlowEngine.CLOSE_WITHDRAW, c.closeKind);
    }

    // ================================================================ 回退：一档、转抄、重来

    @Test
    void rollback_oneStepOnly_regressionOldResetToZeroBug() {
        MemCase c = atMeasureStage(); // 当下第3档
        c.apply(engine.rollback(c.events, "李四", T0, null));
        assertEquals(1, engine.currentStage(c.events), "旧码一律归0，钉死只收一档：3档收到2档");
        c.apply(engine.rollback(c.events, "李四", T0, null));
        assertEquals(0, engine.currentStage(c.events));
        assertThrows(CaseFlowRuleException.class, () -> engine.rollback(c.events, "李四", T0, null));
    }

    @Test
    void rollback_transfersOldNoteVerbatim_andRestartCountsFromScratch() {
        MemCase c = atMeasureStage();
        c.apply(engine.note(c.events, "措施核查：样品待复检", "赵六", T0, null));
        assertEquals("措施核查：样品待复检", c.content);
        c.apply(engine.rollback(c.events, "李四", T0, null));
        // 被收那一档的按语照原先后转进追溯串末尾
        CaseFlowEvent last = c.events.get(c.events.size() - 1);
        assertEquals(CaseFlowEvent.ROLLBACK, last.getActionKind());
        assertEquals("措施核查：样品待复检", last.getNote());
        assertEquals(1, last.getStage(), "转抄笔落在收到的那一档（纳入受限）");

        // 重新挪回措施核查：从头累计，核收要重记、见证凭证都清零重来
        c.apply(engine.recordAccept(c.events, "可", "王五", T0, null));
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertEquals(2, engine.currentStage(c.events));
        assertNull(c.certNo, "新一轮凭证栏是空的，旧结论不顶用");
        CaseFlowEngine.Snapshot snap = engine.takeSnapshot(c.events);
        assertEquals(2, snap.getRounds()[CaseFlowStage.MEASURE_CHECK], "措施核查已是第二轮");
        assertThrows(CaseFlowRuleException.class,
                () -> engine.advance(c.events, facts(true, false), "李四", T0, null));
    }

    // ================================================================ 其三：闭口只读、双活拦截、二次按语

    @Test
    void closedCase_allThreeMutationsRejected_butStillVisible() {
        MemCase c = fullyClosedCase();
        assertThrows(CaseFlowRuleException.class, () -> engine.note(c.events, "再补一句", "李四", T0, null));
        assertThrows(CaseFlowRuleException.class, () -> engine.advance(c.events, facts(true, false), "李四", T0, null));
        assertThrows(CaseFlowRuleException.class,
                () -> engine.withdraw(c.events, "张三", "张三", "收回", T0, null));
        // 名册照常露脸：列表/读不拦，只是不给把手（读动作不抛）
        assertEquals(3, engine.currentStage(c.events));
        assertNotNull(engine.replay(c.events));
    }

    @Test
    void secondNoteSameText_idempotent_differentTextRejected_noCopy() {
        MemCase c = openCase();
        c.apply(engine.note(c.events, "头一句", "李四", T0, "n1"));
        CaseFlowPlan same = engine.note(c.events, "头一句", "李四", T0, "n2");
        assertTrue(same.isIdempotent(), "同原话并着递两回，幂等认下");
        CaseFlowRuleException ex = assertThrows(CaseFlowRuleException.class,
                () -> engine.note(c.events, "第二句", "李四", T0, "n3"));
        assertTrue(ex.getMessage().contains("头一句"));
        assertEquals(1, countKind(c.events, CaseFlowEvent.NOTE), "不另存副本");
        assertEquals("头一句", c.content, "行面只留头一句");
    }

    // ================================================================ 其四：回放只读、缺档缺栏、同刻两笔

    @Test
    void replay_doesNotChangeCurrent_regressionQianNianBug() {
        MemCase c = fullyClosedCase();
        int before = engine.currentStage(c.events);
        List<CaseFlowEvent> copy = new ArrayList<>(c.events);
        CaseFlowReplay replay = engine.replay(c.events,
                CaseFlowEngine.RowSnapshot.of(c.stage, c.status, c.content, c.certNo));
        assertEquals(before, engine.currentStage(c.events), "重演完当下档不许回到第二档");
        assertEquals(copy.size(), c.events.size(), "回放不写一笔");
        assertTrue(replay.isConsistent());
        assertEquals(4, replay.getDerivedStageCount(), "倒着捋出四阶");
        assertTrue(replay.isDerivedClosed());
    }

    @Test
    void replay_namesMissingStageAndColumn() {
        MemCase c = openCase();
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null)); // 到纳入受限，没记可
        CaseFlowReplay replay = engine.replay(c.events);
        assertTrue(replay.getGaps().stream().anyMatch(g -> g.contains("第2档") && g.contains("核收")),
                "缺口要点名第几档、缺哪栏：" + replay.getGaps());
    }

    @Test
    void replay_measureStageNamesMissingWitnessAndCert() {
        MemCase c = atMeasureStage();
        c.apply(engine.witness(c.events, "村监管岗甲", "赵六", T0, null));
        CaseFlowReplay replay = engine.replay(c.events);
        assertTrue(replay.getGaps().stream().anyMatch(g -> g.contains("第3档") && g.contains("第2笔")));
        assertTrue(replay.getGaps().stream().anyMatch(g -> g.contains("凭证串")));
    }

    @Test
    void twoSubmissionsSameInstant_keptAsTwoSeparateStrokes() {
        MemCase c = atMeasureStage();
        // 同刻两条通道各送一笔见证：各记各的，不许并成一条、不许吞掉一条
        c.apply(engine.witness(c.events, "村监管岗甲", "赵六", T0, null));
        c.apply(engine.witness(c.events, "镇复核岗乙", "钱七", T0, null));
        assertEquals(2, countKind(c.events, CaseFlowEvent.WITNESS));
        List<CaseFlowEvent> ws = new ArrayList<>();
        c.events.stream().filter(e -> e.getActionKind().equals(CaseFlowEvent.WITNESS)).forEach(ws::add);
        assertEquals(ws.get(0).getSeq() + 1, ws.get(1).getSeq(), "同刻也分先后两笔");
    }

    @Test
    void sameActionWithSameToken_submittedTwice_isIdempotent() {
        MemCase c = openCase();
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, "adv-1"));
        // 并着递两回同一推进：第二回认下但不挪
        CaseFlowPlan dup = engine.advance(c.events, facts(true, false), "李四", T0, "adv-1");
        assertTrue(dup.isIdempotent());
        assertEquals(1, engine.currentStage(c.events));
        assertEquals(1, countKind(c.events, CaseFlowEvent.ADVANCE));
    }

    // ================================================================ 其五：几个数一处回答、对账

    @Test
    void reconcile_positionalStrokes_matchLedger() {
        MemCase c = fullyClosedCase();
        // 受理1 + 挪入3 = 4 笔位置笔
        CaseFlowEngine.ReconcileResult ok = engine.reconcile(c.events, 4);
        assertTrue(ok.isMatched(), ok.getGaps()::toString);
        CaseFlowEngine.ReconcileResult bad = engine.reconcile(c.events, 3);
        assertFalse(bad.isMatched());
        assertTrue(bad.getGaps().get(0).contains("受理簿笔数对不上"));
    }

    @Test
    void reconcile_detectsJumpStageStroke() {
        MemCase c = new MemCase();
        c.events.add(new CaseFlowEvent(1, 0, 1, CaseFlowEvent.OPEN, "受理", "李四", T0));
        // 伪造一笔越两档的挪入
        c.events.add(new CaseFlowEvent(2, 2, 1, CaseFlowEvent.ADVANCE, "越档", "李四", T0));
        CaseFlowEngine.ReconcileResult r = engine.reconcile(c.events, 2);
        assertFalse(r.isMatched());
        assertTrue(r.getGaps().stream().anyMatch(g -> g.contains("越档笔")));
    }

    @Test
    void reconcile_selfCheckWithoutLedger_isSelfConsistent() {
        MemCase c = atMeasureStage();
        CaseFlowEngine.ReconcileResult r = engine.reconcile(c.events, null);
        assertTrue(r.isMatched(), r.getGaps()::toString);
    }

    // ================================================================ 串读法与当下一致

    @Test
    void replay_stepsOrder_isReverseChronologicalPerCurrentRounds() {
        MemCase c = openCase();
        c.apply(engine.note(c.events, "头档按语", "李四", T0, null));
        c.apply(engine.advance(c.events, facts(true, false), "李四", new Date(T0.getTime() + 1000), null));
        CaseFlowReplay replay = engine.replay(c.events);
        // 倒演：末档（纳入受限）在最前
        assertEquals(1, replay.getSteps().get(0).getStage());
        // 头一档在最后，其按语从旧轮取，不借旁档结论
        CaseFlowReplay.Step last = replay.getSteps().get(replay.getSteps().size() - 1);
        assertEquals(0, last.getStage());
        assertEquals("头档按语", last.getNote());
    }

    @Test
    void replay_rowMismatch_flaggedButNotWrittenBack() {
        MemCase c = openCase();
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        // 主行谎称自己还在头一档（前年那类摆不齐）
        CaseFlowReplay replay = engine.replay(c.events,
                CaseFlowEngine.RowSnapshot.of(0, CaseFlowEngine.STATUS_ACTIVE, null, null));
        assertFalse(replay.isConsistent());
        assertEquals(1, replay.getDerivedStage(), "履历轧出的真值不变，也不写回主行");
        assertEquals(1, engine.currentStage(c.events));
    }

    @Test
    void accept_wrongStageRejected() {
        MemCase c = openCase(); // 还在头一档
        assertThrows(CaseFlowRuleException.class,
                () -> engine.recordAccept(c.events, "可", "王五", T0, null));
    }

    @Test
    void cert_wrongStageAndBlankRejected() {
        MemCase c = openCase();
        assertThrows(CaseFlowRuleException.class,
                () -> engine.cert(c.events, "CERT-1", "孙八", T0, null));
        MemCase m = atMeasureStage();
        assertThrows(CaseFlowRuleException.class,
                () -> engine.cert(m.events, "  ", "孙八", T0, null));
    }

    @Test
    void zoneMergeClose_requiresMergedFact() {
        MemCase c = openCase();
        assertThrows(CaseFlowRuleException.class,
                () -> engine.zoneMergeClose(c.events, facts(true, false), "李四", T0, null));
        c.apply(engine.zoneMergeClose(c.events, facts(true, true), "李四", T0, null));
        assertTrue(engine.isClosed(c.events));
    }

    // ================================================================ 帮手

    private MemCase atMeasureStage() {
        MemCase c = openCase();
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        c.apply(engine.recordAccept(c.events, "可", "王五", T0, null));
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        return c;
    }

    private MemCase fullyClosedCase() {
        MemCase c = atMeasureStage();
        c.apply(engine.witness(c.events, "村监管岗甲", "赵六", T0, null));
        c.apply(engine.witness(c.events, "镇复核岗乙", "钱七", T0, null));
        c.apply(engine.cert(c.events, "CERT-2026-0001", "孙八", T0, null));
        c.apply(engine.advance(c.events, facts(true, false), "李四", T0, null));
        return c;
    }
}
