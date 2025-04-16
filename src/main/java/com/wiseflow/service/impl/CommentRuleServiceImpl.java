package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wiseflow.entity.CommentRule;
import com.wiseflow.entity.DomainCommentRule;
import com.wiseflow.entity.DomainConfig;
import com.wiseflow.mapper.CommentMapper;
import com.wiseflow.mapper.CommentRuleMapper;
import com.wiseflow.mapper.DomainCommentRuleMapper;
import com.wiseflow.service.CommentRuleService;
import com.wiseflow.service.CommentService;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 评论规则服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentRuleServiceImpl extends ServiceImpl<CommentRuleMapper, CommentRule> implements CommentRuleService {

    private final CommentMapper commentMapper;
    private final CommentRuleMapper commentRuleMapper;
    private final DomainConfigService domainConfigService;
    private final DomainCommentRuleMapper domainCommentRuleMapper;

    @Override
    public long count() {
        return commentRuleMapper.selectCount(null);
    }

    @Override
    public CommentRule getById(Long id) {
        return commentRuleMapper.selectById(id);
    }

    @Override
    public boolean saveRule(CommentRule rule) {
        commentRuleMapper.insert(rule);
        return true;
    }

    @Override
    public boolean updateRule(CommentRule rule) {
        commentRuleMapper.updateById(rule);
        return true;
    }

    @Override
    public boolean deleteRule(Long id) {
        commentRuleMapper.deleteById(id);
        return true;
    }


    @Override
    public List<CommentRule> findByDomainConfig(String domainConfig) {
        return commentRuleMapper.findByDomainConfig(domainConfig);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CommentRule> initDefaultRules(String domainConfig) {
        List<CommentRule> defaultRules = new ArrayList<>();

        return defaultRules;
    }

    @Override
    public List<CommentRule> findAllEnabled() {
        return null;
    }

    @Override
    public List<CommentRule> findEnabledByDomainConfig(String domainConfig) {
        return null;
    }

    @Override
    public void toggleStatus(Integer id, Boolean enabled) {

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, Boolean enabled) {
        commentRuleMapper.updateStatus(id, enabled);
        log.info("更新评论规则状态成功: id={}, enabled={}", id, enabled);
    }

    @Override
    public List<CommentRule> findAll() {
        return null;
    }

    @Override
    public CommentRule initDefaultRule(String domainConfig) {
        return null;
    }


    @Override
    public void batchUpdateStatus(List<Integer> ids, Boolean enabled) {

    }

    @Override
    public List<CommentRule> getRulesByDomain(String domain) {
        //查询域名id
        var byDomain = domainConfigService.findByDomain(domain);
        DomainConfig domainConfig = byDomain.get();
        //查询中间表
        List<DomainCommentRule> domainCommentRules = domainCommentRuleMapper.selectList(
            new QueryWrapper<DomainCommentRule>()
                .eq("domain_config_id", domainConfig.getId())
        );
        //查询所有的
        List<CommentRule> commentRules = commentRuleMapper.selectList(
            new QueryWrapper<CommentRule>()
                .eq("enabled", true)
                .orderByAsc("id")
        );
        //设置是否已插入
        commentRules.forEach(commentRule -> {
            commentRule.setIsInserted(domainCommentRules.stream().anyMatch(domainCommentRule -> domainCommentRule.getCommentRuleId().longValue() == commentRule.getId()));
        });
        return commentRules;
    }
} 