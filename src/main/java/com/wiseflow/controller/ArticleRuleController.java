package com.wiseflow.controller;

import com.wiseflow.common.Result;
import com.wiseflow.entity.ArticleRule;
import com.wiseflow.service.ArticleRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章分配规则控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/article/rules")
@RequiredArgsConstructor
public class ArticleRuleController {

    private final ArticleRuleService articleRuleService;

    /**
     * 获取规则列表
     */
    @GetMapping
    public Result<List<ArticleRule>> list() {
        log.info("获取所有文章分配规则");
        List<ArticleRule> rules = articleRuleService.findAll();
        return Result.success(rules);
    }


    /**
     * 获取单个规则
     */
    @GetMapping("/{id}")
    public Result<ArticleRule> get(@PathVariable Long id) {
        log.info("获取文章分配规则: id={}", id);
        ArticleRule rule = articleRuleService.findById(id);
        return Result.success(rule);
    }

    /**
     * 创建规则
     */
    @PostMapping
    public Result<ArticleRule> create(@RequestBody ArticleRule rule) {
        log.info("创建文章分配规则: {}", rule);
        boolean saved = articleRuleService.save(rule);
        if (saved) {
            return Result.success(rule);
        } else {
            return Result.error("创建规则失败");
        }
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public Result<ArticleRule> update(
            @PathVariable Long id,
            @RequestBody ArticleRule rule) {
        log.info("更新文章分配规则: id={}, rule={}", id, rule);
        rule.setId(id);
        ArticleRule updated = articleRuleService.update(rule);
        return Result.success(updated);
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除文章分配规则: id={}", id);
        articleRuleService.delete(id);
        return Result.success();
    }
} 