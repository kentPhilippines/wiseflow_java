package com.wiseflow.service;

import com.wiseflow.core.crawler.WebCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheCleanupService {
    private final WebCrawler webCrawler;
    private final ArticleService articleService;
    
    @Value("${article.expire.days:30}")  // 从配置文件读取过期天数，默认30天
    private int expireDays;

    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    public void cleanupCrawledUrls() {
        try {
            log.info("Starting cleanup of crawled URLs cache");
            webCrawler.clearCrawledUrls();
            log.info("Finished cleanup of crawled URLs cache");
        } catch (Exception e) {
            log.error("Failed to cleanup crawled URLs cache", e);
        }
    }

    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void cleanupExpiredArticles() {
        try {
            log.info("Starting cleanup of expired articles older than {} days", expireDays);
            articleService.deleteExpiredArticles(expireDays);
            log.info("Finished cleanup of expired articles");
        } catch (Exception e) {
            log.error("Failed to cleanup expired articles", e);
        }
    }
} 