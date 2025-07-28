package com.wiseflow.controller;

import com.wiseflow.common.Result;
import com.wiseflow.entity.Article;
import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.entity.ArticleRule;
import com.wiseflow.entity.SeoKeyword;
import com.wiseflow.service.ArticleAiService;
import com.wiseflow.service.ArticleRuleService;
import com.wiseflow.service.ArticleService;
import com.wiseflow.service.NewsService;
import com.wiseflow.service.SeoKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ArticleController {

    private final NewsService newsService;
    private final ArticleRuleService articleRuleService;
    private final ArticleService articleService;
    private final SeoKeywordService seoKeywordService;
    private final ArticleAiService articleAiService;

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