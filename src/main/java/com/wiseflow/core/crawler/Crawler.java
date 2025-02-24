package com.wiseflow.core.crawler;

import com.wiseflow.model.CrawlConfig;
import org.jsoup.nodes.Document;
import java.util.List;

public interface Crawler {
    void crawl(CrawlConfig config);
    boolean isArticlePage(String url);
    List<String> extractLinks(Document doc);
    void clearCrawledUrls();
} 