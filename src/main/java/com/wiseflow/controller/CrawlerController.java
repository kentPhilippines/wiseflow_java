package com.wiseflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.config.CacheConfig;
import com.wiseflow.entity.CrawlLog;
import com.wiseflow.entity.FailedUrl;
import com.wiseflow.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 爬虫控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {
    
    private final CrawlerService crawlerService;
    
    /**
     * 启动爬虫
     * 清除爬虫日志缓存
     */
    @PostMapping("/start/{spiderName}")
    @CacheEvict(value = CacheConfig.CACHE_CRAWLER_LOGS, allEntries = true)
    public ResponseEntity<Void> startCrawler(@PathVariable String spiderName) {
        crawlerService.startCrawler(spiderName);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 停止爬虫
     * 清除爬虫日志缓存
     */
    @PostMapping("/stop/{spiderName}")
    @CacheEvict(value = CacheConfig.CACHE_CRAWLER_LOGS, allEntries = true)
    public ResponseEntity<Void> stopCrawler(@PathVariable String spiderName) {
        crawlerService.stopCrawler(spiderName);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/retry/{spiderName}")
    public ResponseEntity<Void> retryCrawlFailedUrls(@PathVariable String spiderName) {
        crawlerService.retryCrawlFailedUrls(spiderName);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取爬虫日志
     */
    @GetMapping("/logs")
    @Cacheable(value = CacheConfig.CACHE_CRAWLER_LOGS, key = "'page:' + #page + ':size:' + #size")
    public ResponseEntity<IPage<CrawlLog>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<CrawlLog> pageRequest = new Page<>(page + 1, size);
        pageRequest.addOrder(OrderItem.desc("start_time"));
        IPage<CrawlLog> logs = crawlerService.findAllLogs(pageRequest);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/logs/{id}")
    public ResponseEntity<CrawlLog> getLogById(@PathVariable Integer id) {
        CrawlLog log = crawlerService.findLogById(id);
        return ResponseEntity.ok(log);
    }
    
    @GetMapping("/logs/spider/{spiderName}")
    public ResponseEntity<List<CrawlLog>> getLogsBySpiderName(@PathVariable String spiderName) {
        List<CrawlLog> logs = crawlerService.findLogsBySpiderName(spiderName);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/logs/time-range")
    public ResponseEntity<List<CrawlLog>> getLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<CrawlLog> logs = crawlerService.findLogsByTimeRange(startTime, endTime);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/running")
    public ResponseEntity<List<CrawlLog>> getRunningCrawlers() {
        List<CrawlLog> runningCrawlers = crawlerService.findRunningCrawlers();
        return ResponseEntity.ok(runningCrawlers);
    }
    
    /**
     * 获取爬虫统计信息
     */
    @GetMapping("/stats")
    @Cacheable(value = CacheConfig.CACHE_CRAWLER_LOGS, key = "'stats'")
    public ResponseEntity<Map<String, Object>> getCrawlerStats() {
        Map<String, Object> stats = crawlerService.getCrawlerStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/failed-urls")
    public ResponseEntity<IPage<FailedUrl>> getAllFailedUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<FailedUrl> pageRequest = new Page<>(page + 1, size);
        pageRequest.addOrder(OrderItem.desc("create_time"));
        IPage<FailedUrl> failedUrls = crawlerService.findAllFailedUrls(pageRequest);
        return ResponseEntity.ok(failedUrls);
    }
    
    @GetMapping("/failed-urls/{id}")
    public ResponseEntity<FailedUrl> getFailedUrlById(@PathVariable Integer id) {
        FailedUrl failedUrl = crawlerService.findFailedUrlById(id);
        return ResponseEntity.ok(failedUrl);
    }
    
    @GetMapping("/failed-urls/spider/{spiderName}")
    public ResponseEntity<List<FailedUrl>> getFailedUrlsBySpiderName(@PathVariable String spiderName) {
        List<FailedUrl> failedUrls = crawlerService.findFailedUrlsBySpiderName(spiderName);
        return ResponseEntity.ok(failedUrls);
    }
    
    @GetMapping("/failed-urls/unprocessed")
    public ResponseEntity<List<FailedUrl>> getUnprocessedFailedUrls() {
        List<FailedUrl> unprocessedFailedUrls = crawlerService.findUnprocessedFailedUrls();
        return ResponseEntity.ok(unprocessedFailedUrls);
    }
    
    @PostMapping("/failed-urls/{id}/mark-processed")
    public ResponseEntity<Void> markFailedUrlAsProcessed(@PathVariable Integer id) {
        crawlerService.markFailedUrlAsProcessed(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/failed-urls/{id}")
    public ResponseEntity<Void> deleteFailedUrl(@PathVariable Integer id) {
        crawlerService.deleteFailedUrl(id);
        return ResponseEntity.ok().build();
    }
} 