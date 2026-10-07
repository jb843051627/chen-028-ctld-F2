package com.fc.v2.service.ctld;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 回放只演不写：从末一档一关一关倒着演出来的样子。
 * derivedXxx 全部由履历轧出，用来与主行当下各栏对账，不许据此写回。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class CaseFlowReplay {

    /** 由履历倒演出来的当下档数（库存档数 0..3；无履历为 -1） */
    private int derivedStage = -1;
    /** 由履历倒演出来是否已闭上 */
    private boolean derivedClosed;
    /** 倒着捋出来经过的阶数（末档驻留档序数，站头一档为 1） */
    private int derivedStageCount;
    /** 倒序的关步（末档在最前），每步只取当时那一轮的留痕 */
    private final List<Step> steps = new ArrayList<>();
    /** 指名道姓的缺口：「第N档（档名）缺：xx栏」 */
    private final List<String> gaps = new ArrayList<>();
    /** 倒演结果与主行当下各栏对不对得上 */
    private boolean consistent = true;

    public static class Step {
        private int stage;
        private String stageName;
        private int roundNo;
        private String actionKind;
        private String note;
        private String actor;
        private Date writeTime;
        private int seq;

        public int getStage() {
            return stage;
        }

        public void setStage(int stage) {
            this.stage = stage;
        }

        public String getStageName() {
            return stageName;
        }

        public void setStageName(String stageName) {
            this.stageName = stageName;
        }

        public int getRoundNo() {
            return roundNo;
        }

        public void setRoundNo(int roundNo) {
            this.roundNo = roundNo;
        }

        public String getActionKind() {
            return actionKind;
        }

        public void setActionKind(String actionKind) {
            this.actionKind = actionKind;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }

        public Date getWriteTime() {
            return writeTime;
        }

        public void setWriteTime(Date writeTime) {
            this.writeTime = writeTime;
        }

        public int getSeq() {
            return seq;
        }

        public void setSeq(int seq) {
            this.seq = seq;
        }
    }

    public int getDerivedStage() {
        return derivedStage;
    }

    public void setDerivedStage(int derivedStage) {
        this.derivedStage = derivedStage;
    }

    public boolean isDerivedClosed() {
        return derivedClosed;
    }

    public void setDerivedClosed(boolean derivedClosed) {
        this.derivedClosed = derivedClosed;
    }

    public int getDerivedStageCount() {
        return derivedStageCount;
    }

    public void setDerivedStageCount(int derivedStageCount) {
        this.derivedStageCount = derivedStageCount;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }
}
