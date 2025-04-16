package com.wiseflow.controller;

import com.wiseflow.dto.DomainConfigRequest;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/domain")
public class DomainConfigController {

    @Autowired
    private DomainConfigService domainConfigService;

    /**
     * 保存域名SEO关键词配置
     */
    @PostMapping("/{id}/config/keyword")
    public Result saveKeywordConfig(@PathVariable Long id, @RequestBody DomainConfigRequest request) {
        domainConfigService.saveKeywordConfig(id, request);
        return Result.success();
    }

    /**
     * 保存域名评论规则配置
     */
    @PostMapping("/{id}/config/comment")
    public Result saveCommentConfig(@PathVariable Long id, @RequestBody DomainConfigRequest request) {
        domainConfigService.saveCommentConfig(id, request);
        return Result.success();
    }

    /**
     * 保存域名文章配置
     */
    @PostMapping("/{id}/config/article")
    public Result saveArticleConfig(@PathVariable Long id, @RequestBody DomainConfigRequest request) {
        domainConfigService.saveArticleConfig(id, request);
        return Result.success();
    }
} 