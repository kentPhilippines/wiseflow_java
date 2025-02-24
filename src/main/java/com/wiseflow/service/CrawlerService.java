package com.wiseflow.service;

import com.wiseflow.core.crawler.Crawler;
import com.wiseflow.entity.Article;
import com.wiseflow.model.CrawlConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {
    private final Crawler crawler;
    private final List<CrawlConfig> crawlConfigs; // 配置可以从配置文件或数据库加载

    @Scheduled(fixedDelayString = "${crawler.fixed-delay:300000}")
    public void scheduledCrawl() {
        for (CrawlConfig config : crawlConfigs) {
            if (!config.isEnabled()) {
                continue;
            }

            try {
                log.info("Starting crawl for: {}", config.getName());
                 crawler.crawl(config);
                log.info("Finished crawl for: {}", config.getName());

                // 避免频繁请求
                TimeUnit.SECONDS.sleep(5);
            } catch (Exception e) {
                log.error("Failed to crawl: {}", config.getName(), e);
            }
        }
    }
}