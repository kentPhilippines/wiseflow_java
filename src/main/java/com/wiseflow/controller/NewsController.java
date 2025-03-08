package com.wiseflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.config.CacheConfig;
import com.wiseflow.entity.News;
import com.wiseflow.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新闻控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    
    private final NewsService newsService;
    
    @GetMapping
    public ResponseEntity<IPage<News>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "crawlTime") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Page<News> pageRequest = new Page<>(page + 1, size);
        if ("asc".equalsIgnoreCase(direction)) {
            pageRequest.addOrder(OrderItem.asc(sortBy));
        } else {
            pageRequest.addOrder(OrderItem.desc(sortBy));
        }
        
        IPage<News> newsPage = newsService.findAll(pageRequest);
        return ResponseEntity.ok(newsPage);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<News> getNewsById(@PathVariable Integer id) {
        News news = newsService.findById(id).orElseThrow(() -> new RuntimeException("News not found with id: " + id));

        // 增加浏览量
        newsService.incrementViewCount(id);
        
        // 返回已更新浏览量的新闻对象
        return ResponseEntity.ok(getNewsByIdWithCache(id));
    }
    
    /**
     * 获取新闻详情并缓存结果
     * 在增加浏览量后调用此方法，确保浏览量更新不受缓存影响
     */
    @Cacheable(value = CacheConfig.CACHE_NEWS_DETAIL_ADMIN, key = "#id")
    private News getNewsByIdWithCache(Integer id) {
        return newsService.findById(id).orElseThrow(() -> new RuntimeException("News not found with id: " + id));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<IPage<News>> getNewsByCategory(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<News> pageRequest = new Page<>(page + 1, size);
        IPage<News> newsPage = newsService.findByCategoryId(categoryId, pageRequest);
        return ResponseEntity.ok(newsPage);
    }
    
    @GetMapping("/hot-news")
    public ResponseEntity<List<News>> getHotNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<News> pageRequest = new Page<>(page + 1, size);
        List<News> newsList = newsService.findHotNews(pageRequest);
        return ResponseEntity.ok(newsList);
    }
    
    @GetMapping("/recommend")
    public ResponseEntity<IPage<News>> getRecommendNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<News> pageRequest = new Page<>(page + 1, size);
        IPage<News> newsPage = newsService.findRecommendNews(pageRequest);
        return ResponseEntity.ok(newsPage);
    }
    
    @GetMapping("/top")
    public ResponseEntity<List<News>> getTopNews() {
        List<News> topNews = newsService.findTopNews();
        return ResponseEntity.ok(topNews);
    }
    
    @GetMapping("/advanced-search")
    public ResponseEntity<IPage<News>> searchNews(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<News> pageRequest = new Page<>(page + 1, size);
        IPage<News> newsPage = newsService.search(title, categoryId, startDate, endDate, pageRequest);
        return ResponseEntity.ok(newsPage);
    }
    
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeNews(@PathVariable Integer id) {
        newsService.incrementLikeCount(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/top")
    public ResponseEntity<Void> toggleTopNews(
            @PathVariable Integer id,
            @RequestParam boolean isTop) {
        
        newsService.toggleTop(id, isTop);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/hot")
    public ResponseEntity<Void> toggleHotNews(
            @PathVariable Integer id,
            @RequestParam boolean isHot) {
        
        newsService.toggleHot(id, isHot);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/recommend")
    public ResponseEntity<Void> toggleRecommendNews(
            @PathVariable Integer id,
            @RequestParam boolean isRecommend) {
        
        newsService.toggleRecommend(id, isRecommend);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 保存新闻
     * 清除相关缓存
     */
    @PostMapping
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_NEWS_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_HOT_NEWS, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_SEARCH_RESULT, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_NEWS_COUNT, allEntries = true)
    })
    public ResponseEntity<News> saveNews(@RequestBody News news) {
        log.info("保存新闻: {}", news.getTitle());
        return ResponseEntity.ok(newsService.save(news));
    }
    
    /**
     * 更新新闻
     * 清除相关缓存
     */
    @PutMapping("/{id}")
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_NEWS_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_DETAIL, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_DETAIL_API, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_DETAIL_ADMIN, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_HOT_NEWS, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_SEARCH_RESULT, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_NEWS_COUNT, allEntries = true)
    })
    public ResponseEntity<News> updateNews(@PathVariable Integer id, @RequestBody News news) {
        log.info("更新新闻: id={}, title={}", id, news.getTitle());
        news.setId(id);
        return ResponseEntity.ok(newsService.save(news));
    }
    
    /**
     * 删除新闻
     * 清除相关缓存
     */
    @DeleteMapping("/{id}")
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_NEWS_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_DETAIL, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_DETAIL_API, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_DETAIL_ADMIN, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_HOT_NEWS, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_SEARCH_RESULT, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_NEWS_COUNT, allEntries = true)
    })
    public ResponseEntity<Void> deleteNews(@PathVariable Integer id) {
        log.info("删除新闻: id={}", id);
        newsService.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> deleteMultipleNews(@RequestBody List<Integer> ids) {
        newsService.deleteAll(ids);
        return ResponseEntity.ok().build();
    }
} 