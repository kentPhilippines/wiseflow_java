package com.wiseflow.controller;

import com.wiseflow.entity.Article;
import com.wiseflow.entity.Category;
import com.wiseflow.service.ArticleService;
import com.wiseflow.service.CategoryService;
import com.wiseflow.service.CacheCleanupService;
import com.wiseflow.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final CacheCleanupService cacheCleanupService;
    private final SyncService syncService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<Article> articles = categoryId != null ? 
                articleService.findByCategory(categoryId, pageable) : 
                articleService.findAll(pageable);
            
            List<Category> categories = categoryService.findAll();
            
            return ResponseEntity.ok(Map.of(
                "articles", articles,
                "categories", categories,
                "currentCategoryId", categoryId != null ? categoryId : -1
            ));
        } catch (Exception e) {
            log.error("Failed to load article list", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to load articles: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Article> detail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(articleService.findById(id));
        } catch (Exception e) {
            log.error("Failed to load article detail for id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            cacheCleanupService.cleanupCrawledUrls();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "缓存清理成功"
            ));
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "缓存清理失败: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/expired/clear")
    public ResponseEntity<Map<String, Object>> clearExpiredArticles() {
        try {
            cacheCleanupService.cleanupExpiredArticles();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "过期文章清理成功"
            ));
        } catch (Exception e) {
            log.error("Failed to clear expired articles", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "过期文章清理失败: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Article>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<Article> articles = articleService.searchByKeyword(keyword, pageable);
            return ResponseEntity.ok(articles);
        } catch (Exception e) {
            log.error("Failed to search articles with keyword: {}", keyword, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<List<Object[]>> getStats(
            @RequestParam(required = false, defaultValue = "7") int days) {
        try {
            List<Object[]> stats = articleService.getArticleStats(days);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get article stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<Void> syncArticle(@PathVariable Long id) {
        try {
            Article article = articleService.findById(id);
            syncService.asyncSyncArticle(article);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to sync article: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 