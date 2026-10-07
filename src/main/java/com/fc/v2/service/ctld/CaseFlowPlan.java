package com.fc.v2.service.ctld;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎算定的一笔走法：挪到哪一档、主行各栏写成什么、追加上哪几笔履历。
 * Service 照单落库，自己不另拿主意——挪档与留痕同出这一张单，口径天然一致。
 * idempotent=true 表示同一动作并着递了两回：库面不改，也不算错。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class CaseFlowPlan {

    private boolean changed;
    private boolean idempotent;
    private Integer stage;
    private Integer status;
    private String content;
    private boolean contentTouched;
    private String certNo;
    private boolean certTouched;
    private Integer closeKind;
    private String lastAction;
    private final List<CaseFlowEvent> appendEvents = new ArrayList<>();

    public static CaseFlowPlan noop() {
        return new CaseFlowPlan();
    }

    public static CaseFlowPlan idempotent() {
        CaseFlowPlan p = new CaseFlowPlan();
        p.idempotent = true;
        return p;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean isIdempotent() {
        return idempotent;
    }

    public void setIdempotent(boolean idempotent) {
        this.idempotent = idempotent;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.contentTouched = true;
    }

    public boolean isContentTouched() {
        return contentTouched;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
        this.certTouched = true;
    }

    public boolean isCertTouched() {
        return certTouched;
    }

    public Integer getCloseKind() {
        return closeKind;
    }

    public void setCloseKind(Integer closeKind) {
        this.closeKind = closeKind;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public List<CaseFlowEvent> getAppendEvents() {
        return appendEvents;
    }
}
