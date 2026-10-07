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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 超标地块纳入受限与解除办件流转单对象 t_ctld_case_flow
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_ctld_case_flow")
@ApiModel(value = "TCtldCaseFlow", description = "超标地块纳入受限与解除办件流转单")
public class TCtldCaseFlow implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 超标地块纳入受限与解除的办件件次 */
    @TableField("biz_no")
    @ApiModelProperty(value = "超标地块纳入受限与解除的办件件次")
    private String bizNo;

    /** 地块码（起件四样之一） */
    @TableField("plot_no")
    @ApiModelProperty(value = "地块码（起件四样之一）")
    private String plotNo;

    /** 所属分区代号（起件四样之一，读分区册对名） */
    @TableField("zone_no")
    @ApiModelProperty(value = "所属分区代号（起件四样之一）")
    private String zoneNo;

    /** 土壤镉实测那一个数（起件四样之一） */
    @TableField("cad_value")
    @ApiModelProperty(value = "土壤镉实测那一个数（起件四样之一）")
    private BigDecimal cadValue;

    /** 拟采用的措施名目（起件四样之一） */
    @TableField("measure_name")
    @ApiModelProperty(value = "拟采用的措施名目（起件四样之一）")
    private String measureName;

    /** 措施核验凭证那一串（进解除复核时写回本行） */
    @TableField("cert_no")
    @ApiModelProperty(value = "措施核验凭证那一串（进解除复核时写回本行）")
    private String certNo;

    /** 递件人（其收回反映即视同收口） */
    @TableField("applicant")
    @ApiModelProperty(value = "递件人")
    private String applicant;

    /** 收口来路 null未收口 0走满末档 1递件人收回 2分区并走 */
    @TableField("close_kind")
    @ApiModelProperty(value = "收口来路 null未收口 0走满末档 1递件人收回 2分区并走")
    private Integer closeKind;

    /** 活件占位键：未闭口件与地块码一一对应，闭口置空 */
    @TableField("open_key")
    @ApiModelProperty(value = "活件占位键，闭口置空")
    private String openKey;

    /** 当前关次 0..3（初核受理/纳入受限/措施核查/解除复核）——由后端按履历轧出，报文序号只作参考 */
    @TableField("stage")
    @ApiModelProperty(value = "当前关次 0..3（初核受理/纳入受限/措施核查/解除复核）")
    private Integer stage;

    /** 单子走到哪一关 0未起 1在办 2已封住 */
    @TableField("status")
    @ApiModelProperty(value = "单子走到哪一关 0未起 1在办 2已封住")
    private Integer status;

    /** 一关一记 */
    @TableField("content")
    @ApiModelProperty(value = "一关一记")
    private String content;

    /** 最近一次挪关动作 */
    @TableField("last_action")
    @ApiModelProperty(value = "最近一次挪关动作")
    private String lastAction;

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

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public String getPlotNo() {
        return plotNo;
    }

    public void setPlotNo(String plotNo) {
        this.plotNo = plotNo;
    }

    public String getZoneNo() {
        return zoneNo;
    }

    public void setZoneNo(String zoneNo) {
        this.zoneNo = zoneNo;
    }

    public BigDecimal getCadValue() {
        return cadValue;
    }

    public void setCadValue(BigDecimal cadValue) {
        this.cadValue = cadValue;
    }

    public String getMeasureName() {
        return measureName;
    }

    public void setMeasureName(String measureName) {
        this.measureName = measureName;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public Integer getCloseKind() {
        return closeKind;
    }

    public void setCloseKind(Integer closeKind) {
        this.closeKind = closeKind;
    }

    public String getOpenKey() {
        return openKey;
    }

    public void setOpenKey(String openKey) {
        this.openKey = openKey;
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
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
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
