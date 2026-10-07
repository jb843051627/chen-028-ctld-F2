package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 超标地块办件变更履历对象 t_ctld_case_flow_trace
 * <p>一关一记，只追加、不改写、不删除。当前关次、逆捋阶次、回放取数统统由这张表轧出。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
@TableName("t_ctld_case_flow_trace")
@ApiModel(value = "TCtldCaseFlowTrace", description = "超标地块办件变更履历（一关一记·只追加）")
public class TCtldCaseFlowTrace implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属办件 t_ctld_case_flow.id */
    @TableField("case_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属办件")
    private Long caseId;

    /** 办件件次（冗余，便于审计取数） */
    @TableField("biz_no")
    @ApiModelProperty(value = "办件件次")
    private String bizNo;

    /** 时序笔次（小者在前，同刻两笔各记各，不许并笔、不许吞笔） */
    @TableField("seq")
    @ApiModelProperty(value = "时序笔次")
    private Integer seq;

    /** 落在哪一关 0..3 */
    @TableField("stage")
    @ApiModelProperty(value = "落在哪一关 0..3")
    private Integer stage;

    /** 该关第几轮驻留（回退后再挪到本关从头累计，从1起） */
    @TableField("round_no")
    @ApiModelProperty(value = "该关第几轮驻留")
    private Integer roundNo;

    /** 动作名目 OPEN受理/NOTE按语/ACCEPT核收可/WITNESS见证/CERT凭证/WITHDRAW收回/ZONE_MERGE并走/ADVANCE挪入/ROLLBACK挪回/CLOSE闭口 */
    @TableField("action_kind")
    @ApiModelProperty(value = "动作名目")
    private String actionKind;

    /** 这一笔的原话（按语/凭证串/收口来由，照原样留底） */
    @TableField("note")
    @ApiModelProperty(value = "这一笔的原话")
    private String note;

    /** 落笔者 */
    @TableField("actor")
    @ApiModelProperty(value = "落笔者")
    private String actor;

    /** 提交那一刻（按此时序倒演） */
    @TableField("write_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "提交那一刻")
    private Date writeTime;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
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

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
