package com.fc.v2.service.ctld;

import java.math.BigDecimal;

/**
 * 起一件交来的四样：地块码、所属分区代号、土壤镉实测那一个数、拟采用的措施名目。
 * 另带递件人，日后只有本人能把反映收回去。
 *
 * @author fuce
 * @date 2026-10-06
 */
public class CaseFlowCreateCmd {

    private String plotNo;
    private String zoneNo;
    private BigDecimal cadValue;
    private String measureName;
    private String applicant;

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

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }
}
