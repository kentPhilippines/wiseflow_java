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

    /**
     * 根据关键词规则改写文章
     */
    @PostMapping("/rewrite-with-keywords")
    public Result<String> rewriteArticleWithKeywords(@RequestParam("articleId") Long articleId,
                                              @RequestParam(value = "keywordIds", required = false) List<Long> keywordIds,
                                              @RequestParam(value = "domain", required = false) String domain) {
        try {
            // 获取文章信息
            Article article = articleService.findById(articleId);
            if (article == null) {
                return Result.error("文章不存在");
            }
            
            List<SeoKeyword> keywords = new ArrayList<>();
            
            // 如果提供了特定的关键词ID列表，则使用这些关键词
            if (keywordIds != null && !keywordIds.isEmpty()) {
                for (Long keywordId : keywordIds) {
                    SeoKeyword keyword = seoKeywordService.findById(keywordId);
                    if (keyword != null) {
                        keywords.add(keyword);
                    }
                }
            } 
            // 如果提供了域名，则使用该域名下的所有关键词
            else if (domain != null && !domain.isEmpty()) {
                keywords = seoKeywordService.getKeywordsByDomain(domain);
            }
            
            if (keywords.isEmpty()) {
                return Result.error("未找到可用的关键词");
            }
            
            // 提交改写任务
            CompletableFuture<ArticleRewrite> future = articleAiService.processArticleWithKeywords(
                    articleId,
                    article.getTitle(),
                    article.getContent(),
                    keywords
            );
            
            // 异步处理，立即返回结果
            return Result.success("文章改写任务已提交，请稍后查看结果");
            
        } catch (Exception e) {
            log.error("根据关键词规则改写文章失败: {}", e.getMessage(), e);
            return Result.error("改写文章失败: " + e.getMessage());
        }
    }
} 