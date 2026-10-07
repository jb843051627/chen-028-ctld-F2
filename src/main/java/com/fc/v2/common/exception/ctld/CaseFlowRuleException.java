package com.fc.v2.common.exception.ctld;

/**
 * 超标地块办件流转规则破例：凡是被规矩挡回的（缺样、越档、门槛不齐、闭口件不动、
 * 第二句按语不收等），一律抛这一种，消息必须点到具体的档与栏，不许只回一句“信息不全”。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class CaseFlowRuleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CaseFlowRuleException(String message) {
        super(message);
    }
}
