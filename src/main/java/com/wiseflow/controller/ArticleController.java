package com.wiseflow.controller;

import com.wiseflow.common.Result;
import com.wiseflow.entity.ArticleRule;
import com.wiseflow.service.ArticleRuleService;
import com.wiseflow.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArticleController {

    private final NewsService newsService;
    private final ArticleRuleService articleRuleService;

    @GetMapping("/articles/list")
    public List<ArticleRule> getArticles(@RequestParam String domain) {
        return articleRuleService.getArticlesByDomain(domain);
    }

    @GetMapping("/article/stats")
    public Result<List<Map<String, Object>>> getArticleStats() {
        List<Map<String, Object>> stats = newsService.getArticleCounts();
        return Result.success(stats);
    }

    @GetMapping("/article/types")
    public Result<List<Map<String, Object>>> getArticleTypes() {
        List<Map<String, Object>> types = newsService.getArticleTypes();
        return Result.success(types);
    }
} 