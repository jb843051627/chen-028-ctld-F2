package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TCtldCaseFlowMapper;
import com.fc.v2.model.auto.TCtldCaseFlow;
import com.fc.v2.service.ITCtldCaseFlowService;

/**
 * 超标地块纳入受限与解除办件流转单 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TCtldCaseFlowServiceImpl implements ITCtldCaseFlowService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TCtldCaseFlowMapper ctldCaseFlowMapper;

    @Override
    public TCtldCaseFlow selectTCtldCaseFlowById(Long id) {
        return this.ctldCaseFlowMapper.selectById(id);
    }

    @Override
    public List<TCtldCaseFlow> selectTCtldCaseFlowList(QueryWrapper<TCtldCaseFlow> queryWrapper) {
        return this.ctldCaseFlowMapper.selectList(queryWrapper);
    }

    @Override
    public TCtldCaseFlow advance(Long id, String remark) {
        TCtldCaseFlow r = this.ctldCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.ctldCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public TCtldCaseFlow rollback(Long id, String remark) {
        TCtldCaseFlow r = this.ctldCaseFlowMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.ctldCaseFlowMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TCtldCaseFlow r = this.ctldCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.ctldCaseFlowMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TCtldCaseFlow r = this.ctldCaseFlowMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.ctldCaseFlowMapper.deleteById(id) > 0;
    }

}
