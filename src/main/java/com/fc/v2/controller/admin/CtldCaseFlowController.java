package com.fc.v2.controller.admin;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TCtldCaseFlow;
import com.fc.v2.service.ITCtldCaseFlowService;
import com.fc.v2.service.ctld.CaseFlowCreateCmd;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 超标地块纳入受限与解除办件流转单 Controller。
 * 进出名目与留痕规矩全在 Service 背后的引擎一处定，这里只受理报面、回原话。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Api(value = "超标地块纳入受限与解除办件流转单")
@Controller
@RequestMapping("/ctldCaseFlow")
public class CtldCaseFlowController extends BaseController {

    private final String prefix = "admin/ctldCaseFlow";

    @Autowired
    private ITCtldCaseFlowService ctldCaseFlowService;

    @ApiOperation(value = "流转台账跳转", notes = "流转台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("ctldCaseFlow:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "超标地块办件流转台账", action = "list")
    @ApiOperation(value = "流转台账（台账与未闭口件数同一回给出）", notes = "流转台账")
    @GetMapping("/list")
    @RequiresPermissions("ctldCaseFlow:list")
    @ResponseBody
    public ResultTable list(TCtldCaseFlow record) {
        QueryWrapper<TCtldCaseFlow> queryWrapper = new QueryWrapper<TCtldCaseFlow>()
                .eq("del_flag", 0)
                .orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<TCtldCaseFlow> page =
                new com.github.pagehelper.PageInfo<TCtldCaseFlow>(ctldCaseFlowService.selectTCtldCaseFlowList(queryWrapper));
        // 屏上那个“没闭上的条数”与台账同一回给出，前端不得另起一套数法。
        return pageTable(page.getList(), page.getTotal(), ctldCaseFlowService.countOpen());
    }

    private ResultTable pageTable(Object data, long total, long openCount) {
        return ResultTable.pageTable(total, data, openCount);
    }

    @Log(title = "超标地块办件受理起件", action = "create")
    @ApiOperation(value = "受理起件（四样齐身）", notes = "地块码/分区代号/镉实测数/措施名目")
    @PostMapping("/create")
    @RequiresPermissions("ctldCaseFlow:create")
    @ResponseBody
    public AjaxResult create(String plotNo, String zoneNo, BigDecimal cadValue, String measureName,
                             String applicant, String idemToken) {
        CaseFlowCreateCmd cmd = new CaseFlowCreateCmd();
        cmd.setPlotNo(plotNo);
        cmd.setZoneNo(zoneNo);
        cmd.setCadValue(cadValue);
        cmd.setMeasureName(measureName);
        cmd.setApplicant(applicant);
        return AjaxResult.success(200, "受理起件", ctldCaseFlowService.create(cmd, idemToken));
    }

    @Log(title = "超标地块办件本关按语", action = "note")
    @ApiOperation(value = "本关留一句按语", notes = "同档第二句：同原话幂等，异原话挡回")
    @PostMapping("/note")
    @RequiresPermissions("ctldCaseFlow:note")
    @ResponseBody
    public AjaxResult note(Long id, String text, String idemToken) {
        return AjaxResult.success(200, "按语已留", ctldCaseFlowService.note(id, text, idemToken));
    }

    @Log(title = "超标地块办件受限核收", action = "accept")
    @ApiOperation(value = "纳入受限档核收", notes = "记“可”才可往措施核查走")
    @PostMapping("/accept")
    @RequiresPermissions("ctldCaseFlow:accept")
    @ResponseBody
    public AjaxResult accept(Long id, String result, String idemToken) {
        return AjaxResult.success(200, "核收已记", ctldCaseFlowService.accept(id, result, idemToken));
    }

    @Log(title = "超标地块办件见证落笔", action = "witness")
    @ApiOperation(value = "措施核查档见证岗落笔", notes = "两名不同见证岗各一笔")
    @PostMapping("/witness")
    @RequiresPermissions("ctldCaseFlow:witness")
    @ResponseBody
    public AjaxResult witness(Long id, String witnessPost, String idemToken) {
        return AjaxResult.success(200, "见证已落", ctldCaseFlowService.witness(id, witnessPost, idemToken));
    }

    @Log(title = "超标地块办件凭证写回", action = "cert")
    @ApiOperation(value = "措施核验凭证串写回本行", notes = "只在措施核查档收")
    @PostMapping("/cert")
    @RequiresPermissions("ctldCaseFlow:cert")
    @ResponseBody
    public AjaxResult cert(Long id, String certNo, String idemToken) {
        return AjaxResult.success(200, "凭证串已写回", ctldCaseFlowService.cert(id, certNo, idemToken));
    }

    /**
     * 推进一档。旧口出入参一字不动（Long id, String remark）；idemToken 是新增的可选投递码，
     * 不带照旧，带上则同一件并着递两回幂等。
     */
    @Log(title = "超标地块办件流转单推进", action = "advance")
    @ApiOperation(value = "推进一档", notes = "一次只跨一档，门槛点着过，走满末档即闭上")
    @PostMapping("/advance")
    @RequiresPermissions("ctldCaseFlow:advance")
    @ResponseBody
    public AjaxResult advance(Long id, String remark, String idemToken) {
        return AjaxResult.success(200, "已推进", ctldCaseFlowService.advance(id, remark, idemToken));
    }

    /** 回退一档。旧口出入参一字不动，idemToken 可选。 */
    @Log(title = "超标地块办件流转单回退", action = "rollback")
    @ApiOperation(value = "回退一档", notes = "一次只收一档，被收档按语照原样转进追溯串末尾")
    @PostMapping("/rollback")
    @RequiresPermissions("ctldCaseFlow:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String remark, String idemToken) {
        return AjaxResult.success(200, "已回退", ctldCaseFlowService.rollback(id, remark, idemToken));
    }

    @Log(title = "超标地块办件递件人收回", action = "withdraw")
    @ApiOperation(value = "递件人收回反映（视同收口）", notes = "只认递件本人")
    @PostMapping("/withdraw")
    @RequiresPermissions("ctldCaseFlow:withdraw")
    @ResponseBody
    public AjaxResult withdraw(Long id, String reason, String idemToken) {
        return AjaxResult.success(200, "已收口", ctldCaseFlowService.withdraw(id, reason, idemToken));
    }

    @Log(title = "超标地块办件分区并走收口", action = "zoneMergeClose")
    @ApiOperation(value = "所属分区并走，视同收口", notes = "以分区册在册状态为准")
    @PostMapping("/zoneMergeClose")
    @RequiresPermissions("ctldCaseFlow:withdraw")
    @ResponseBody
    public AjaxResult zoneMergeClose(Long id, String idemToken) {
        return AjaxResult.success(200, "已按分区并走收口", ctldCaseFlowService.zoneMergeClose(id, idemToken));
    }

    @ApiOperation(value = "变更履历（审计取数来路）", notes = "按时序，只增不改")
    @GetMapping("/trace")
    @RequiresPermissions("ctldCaseFlow:replay")
    @ResponseBody
    public AjaxResult trace(Long id) {
        return AjaxResult.successData(200, ctldCaseFlowService.traces(id));
    }

    @ApiOperation(value = "倒着重演一遍", notes = "从末档一关一关倒演，只看结果、不写回；缺第几档缺哪栏逐一点名")
    @GetMapping("/replay")
    @RequiresPermissions("ctldCaseFlow:replay")
    @ResponseBody
    public AjaxResult replay(Long id) {
        return AjaxResult.successData(200, ctldCaseFlowService.replay(id));
    }

    @ApiOperation(value = "倒捋档数与受理簿对账", notes = "ledgerStrokes 不传按本件位置笔自核")
    @GetMapping("/reconcile")
    @RequiresPermissions("ctldCaseFlow:replay")
    @ResponseBody
    public AjaxResult reconcile(Long id, Integer ledgerStrokes) {
        return AjaxResult.successData(200, ctldCaseFlowService.reconcile(id, ledgerStrokes));
    }

    @ApiOperation(value = "件详情", notes = "当下各栏照库面回，档数以履历轧的为准")
    @GetMapping("/get")
    @RequiresPermissions("ctldCaseFlow:view")
    @ResponseBody
    public AjaxResult get(Long id) {
        return AjaxResult.successData(200, ctldCaseFlowService.selectTCtldCaseFlowById(id));
    }
}
