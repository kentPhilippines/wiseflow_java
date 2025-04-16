package com.wiseflow.controller;

import com.wiseflow.entity.CommentRule;
import com.wiseflow.service.CommentRuleService;
import com.wiseflow.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论规则控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentRuleController {

    private final CommentRuleService commentRuleService;

    /**
     * 获取所有评论规则
     */
    @GetMapping
    public Result<List<CommentRule>> list() {
        List<CommentRule> rules = commentRuleService.list();
        return Result.success(rules);
    }

    /**
     * 根据ID获取评论规则
     */
    @GetMapping("/{id}")
    public Result<CommentRule> getById(@PathVariable Long id) {
        CommentRule rule = commentRuleService.getById(id);
        return Result.success(rule);
    }

    /**
     * 保存评论规则
     */
    @PostMapping
    public Result<CommentRule> save(@RequestBody CommentRule rule) {
        boolean success = commentRuleService.saveRule(rule);
        return success ? Result.success(rule) : Result.error("保存失败");
    }

    /**
     * 更新评论规则
     */
    @PutMapping("/{id}")
    public Result<CommentRule> update(@PathVariable Long id, @RequestBody CommentRule rule) {
        rule.setId(id);
        boolean success = commentRuleService.updateRule(rule);
        return success ? Result.success(rule) : Result.error("更新失败");
    }

    /**
     * 删除评论规则
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = commentRuleService.deleteRule(id);
        return success ? Result.success() : Result.error("删除失败");
    }

    /**
     * 获取指定域名的评论规则
     */
    @GetMapping("/rules")
    public List<CommentRule> getRules(@RequestParam String domain) {
        return commentRuleService.getRulesByDomain(domain);
    }

    /**
     * 获取所有启用的评论规则
     */
    @GetMapping("/enabled")
    public Result<List<CommentRule>> getAllEnabled() {
        List<CommentRule> rules = commentRuleService.findAllEnabled();
        return Result.success(rules);
    }

    /**
     * 初始化域名配置的默认评论规则
     */
    @PostMapping("/init/{domainConfig}")
    public Result<List<CommentRule>> initDefaultRules(@PathVariable String domainConfig) {
        List<CommentRule> rules = commentRuleService.initDefaultRules(domainConfig);
        return Result.success(rules);
    }
    
    /**
     * 初始化单个默认规则
     */
    @PostMapping("/init-default/{domainConfig}")
    public Result<CommentRule> initDefaultRule(@PathVariable String domainConfig) {
        CommentRule rule = commentRuleService.initDefaultRule(domainConfig);
        return Result.success(rule);
    }

    /**
     * 启用/禁用评论规则
     */
    @PutMapping("/toggle/{id}")
    public Result<Void> toggleStatus(
            @PathVariable Integer id, 
            @RequestParam Boolean enabled) {
        
        commentRuleService.toggleStatus(id, enabled);
        return Result.success();
    }
    
    /**
     * 获取指定域名的启用规则
     */
    @GetMapping("/enabled/{domainConfig}")
    public Result<List<CommentRule>> getEnabledByDomain(@PathVariable String domainConfig) {
        List<CommentRule> rules = commentRuleService.findEnabledByDomainConfig(domainConfig);
        return Result.success(rules);
    }
    
    /**
     * 批量更新评论规则状态
     */
    @PutMapping("/batch/status")
    public Result<Void> batchUpdateStatus(
            @RequestParam List<Integer> ids,
            @RequestParam Boolean enabled) {
        
        commentRuleService.batchUpdateStatus(ids, enabled);
        return Result.success();
    }
} 