package com.wiseflow.service;

import com.wiseflow.entity.Article;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {
    private final RestTemplate restTemplate;
    private final ArticleService articleService;

    @Value("${sync.api.base-url:http://www.spider-seo.xyz}")
    private String syncBaseUrl;

    private String getSyncUrl(String categoryName) {
        if ("网易体育".equals(categoryName)) {
            return syncBaseUrl + "/news/";
        } else if ("彩票资讯".equals(categoryName)) {
            return syncBaseUrl + "/news/caipiao";
        } else if ("直播吧足球".equals(categoryName)) {
            return syncBaseUrl + "/news/";
        }
        return syncBaseUrl + "/news/"; // 默认同步到新闻接口
    }

    // 每5分钟执行一次同步
    @Scheduled(fixedRate = 300000)
    public void scheduledSync() {
        log.info("Starting scheduled sync task");
        try {
            // 获取未同步的文章
            List<Article> unsyncedArticles = articleService.findUnsyncedArticles();
            if (unsyncedArticles.isEmpty()) {
                log.info("No unsynced articles found");
                return;
            }

            // 异步同步每篇文章
            unsyncedArticles.forEach(this::asyncSyncArticle);
            
            log.info("Scheduled sync task completed, processed {} articles", unsyncedArticles.size());
        } catch (Exception e) {
            log.error("Error in scheduled sync task", e);
        }
    }

    @Async
    public void asyncSyncArticle(Article article) {
        try {
            // 构建同步数据
            Map<String, Object> syncData = new HashMap<>();
            syncData.put("title", article.getTitle());
            syncData.put("author", article.getAuthor());
            syncData.put("publish_time", article.getCreatedAt().format(DateTimeFormatter.BASIC_ISO_DATE));
            syncData.put("content", article.getContent());
            syncData.put("images", article.getImages().stream()
                .map(img -> img.startsWith("//") ? "https:" + img : img)
                .collect(Collectors.toList()));
            syncData.put("abstract", article.getSummary());
            syncData.put("url", article.getUrl());

            // 根据分类获取同步接口URL
            String syncUrl = getSyncUrl(article.getCategoryName());

            // 发送同步请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(syncData, headers);
            
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(syncUrl, request, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    // 更新文章同步状态
                    article.setSynced(true);
                    article.setSyncTime(LocalDateTime.now());
                    articleService.save(article);
                    log.info("Successfully synced article to {}: {}", syncUrl, article.getTitle());
                } else {
                    log.error("Failed to sync article to {}: {}, status: {}", 
                        syncUrl, article.getTitle(), response.getStatusCode());
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("HTTP error syncing article to {}: {}, status: {}, response: {}", 
                    syncUrl, article.getTitle(), e.getStatusCode(), e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.error("Error syncing article: {}", article.getTitle(), e);
        }
    }
}