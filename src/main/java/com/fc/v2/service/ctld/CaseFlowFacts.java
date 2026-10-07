package com.fc.v2.service.ctld;

/**
 * 放行门槛要向库外核实的事实，由 Service 在件锁内查来交给引擎；
 * 引擎本身不碰库，只管“事实齐不齐、该不该放行”。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class CaseFlowFacts {

    /** 地块在底册上还挂着名（行在、未删就算挂名；撤出的进不去） */
    private boolean plotRegistered;
    /** 所属分区已从分区册并走撤出 */
    private boolean zoneMerged;

    public static CaseFlowFacts of(boolean plotRegistered, boolean zoneMerged) {
        CaseFlowFacts f = new CaseFlowFacts();
        f.plotRegistered = plotRegistered;
        f.zoneMerged = zoneMerged;
        return f;
    }

    public boolean isPlotRegistered() {
        return plotRegistered;
    }

    public void setPlotRegistered(boolean plotRegistered) {
        this.plotRegistered = plotRegistered;
    }

    public boolean isZoneMerged() {
        return zoneMerged;
    }

    public void setZoneMerged(boolean zoneMerged) {
        this.zoneMerged = zoneMerged;
    }
}
