package com.fc.v2.service.ctld;

import java.util.Date;

/**
 * 履历上的一笔：一关那一句按语连同提交那一刻。只增不改不删，
 * 当前档数、能不能放行、逆捋阶次、回放取数全部从这些笔轧出来。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class CaseFlowEvent {

    /** 受理起件 */
    public static final String OPEN = "OPEN";
    /** 按语 */
    public static final String NOTE = "NOTE";
    /** 纳入受限核收记「可」 */
    public static final String ACCEPT = "ACCEPT";
    /** 见证岗落一笔 */
    public static final String WITNESS = "WITNESS";
    /** 措施核验凭证串写回本行 */
    public static final String CERT = "CERT";
    /** 递件人收回反映 */
    public static final String WITHDRAW = "WITHDRAW";
    /** 所属分区从分区册并走 */
    public static final String ZONE_MERGE = "ZONE_MERGE";
    /** 往前挪一关 */
    public static final String ADVANCE = "ADVANCE";
    /** 往后收一关（note 里照原样转抄被收那一档留下的按语） */
    public static final String ROLLBACK = "ROLLBACK";
    /** 闭口封件 */
    public static final String CLOSE = "CLOSE";

    private Long id;
    private int seq;
    private int stage;
    private int roundNo;
    private String actionKind;
    private String note;
    private String actor;
    private Date writeTime;
    /** 投递幂等码（落履历 remark 栏）：同一件同一码并递两回，第二回原样认下、不再挪档 */
    private String token;

    public CaseFlowEvent() {
    }

    public CaseFlowEvent(int seq, int stage, int roundNo, String actionKind, String note, String actor, Date writeTime) {
        this.seq = seq;
        this.stage = stage;
        this.roundNo = roundNo;
        this.actionKind = actionKind;
        this.note = note;
        this.actor = actor;
        this.writeTime = writeTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
