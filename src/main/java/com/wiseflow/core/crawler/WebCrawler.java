package com.wiseflow.core.crawler;

import com.wiseflow.entity.Article;
import com.wiseflow.entity.Category;
import com.wiseflow.model.CrawlConfig;
import com.wiseflow.core.parser.Parser;
import com.wiseflow.core.storage.Storage;
import com.wiseflow.service.ArticleService;
import com.wiseflow.service.CategoryService;
import com.wiseflow.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebCrawler implements Crawler {
    private final Parser parser;
    private final Storage storage;
    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final SyncService syncService;
    private final Set<String> crawledUrls = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void crawl(CrawlConfig config) {
        try {
            Document doc = Jsoup.connect(config.getUrl())
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeout())
                    .get();

            if (isArticlePage(config.getUrl())) {
                processArticlePage(doc, config);
            } else {
                List<String> links = extractLinks(doc);
                for (String link : links) {
                    if (!shouldProcessUrl(link)) {
                        continue;
                    }
                    
                    try {
                        Document articleDoc = Jsoup.connect(link)
                                .userAgent(config.getUserAgent())
                                .timeout(config.getTimeout())
                                .get();
                        processArticlePage(articleDoc, config);
                    } catch (Exception e) {
                        log.error("Failed to crawl article: {}", link, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to crawl: {}", config.getUrl(), e);
        }
    }

    private boolean shouldProcessUrl(String url) {
        // 检查URL是否已经爬取过或已存在于数据库
        boolean urlExists = articleService.isUrlExists(url);
        boolean contains = crawledUrls.contains(url);
        log.info(crawledUrls.toString());
        if (contains || urlExists) {
            log.debug("Skip duplicate URL urlExists: {}", urlExists);
            log.debug("Skip duplicate URL contains : {} ", contains);
            log.debug("Skip duplicate URL  : {} ", url);
            return false;
        }
        
        // 添加到已爬取集合
        crawledUrls.add(url);
        return true;
    }

    private void processArticlePage(Document doc, CrawlConfig config) {
        String url = doc.location();
        try {
            // 设置文档的基础URL，用于解析相对路径
            doc.setBaseUri(url);
            
            Article article = parser.parse(doc, config.getParseRule());
            if (article != null) {
                // 处理分类
                String categoryName = config.getParseRule().getCategoryName();
                if (!StringUtils.hasText(categoryName)) {
                    categoryName = "未分类";
                }
                
                try {
                    // 获取或创建分类
                    Category category = categoryService.findByName(categoryName);
                    if (category == null) {
                        category = new Category();
                        category.setName(categoryName);
                        category.setDescription("自动创建的分类");
                        category = categoryService.save(category);
                        log.info("Created new category: {}", categoryName);
                    }
                    
                    // 设置文章分类
                    article.setCategory(category);
                    article.setCategoryName(category.getName());
                } catch (Exception e) {
                    log.error("Failed to process category for article: {}", url, e);
                    // 设置默认分类名称
                    article.setCategoryName("未分类");
                }

                // 保存文章
                Article savedArticle = articleService.save(article);
                if (savedArticle != null) {
                    storage.save(savedArticle);
                    syncService.asyncSyncArticle(savedArticle);
                    log.info("Successfully saved and synced article: {}", article.getTitle());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process article: {}", url, e);
        }
    }

    @Override
    public boolean isArticlePage(String url) {
        return isValidLink(url);
    }

    @Override
    public List<String> extractLinks(Document doc) {
        List<String> links = new ArrayList<>();
        String baseUrl = doc.baseUri();
        
        // 根据不同网站使用不同的选择器
        Elements elements;
        if (baseUrl.contains("vipc.cn")) {
            // 提取文章列表中的链接
            elements = doc.select("a[href*=/article/]");  // 文章链接
            elements.addAll(doc.select("a[href*=/digit/]"));  // 数字彩频道链接
        } else if (baseUrl.contains("zhibo8.com")) {
            // 直播吧足球新闻链接
            elements = doc.select("a[href*=/zuqiu/]");
        } else {
            elements = doc.select("a[href*=/article/], a[href*=/news/], a[href*=/digit/]");
        }

        elements.forEach(element -> {
            String link = element.attr("abs:href");
            if (!links.contains(link) && isValidLink(link)) {
                links.add(link);
            }
        });
        return links;
    }

    private boolean isValidLink(String url) {
        if (url == null) return false;
        
        // 唯彩看球链接规则
        if (url.contains("vipc.cn")) {
            return (url.contains("/article/") || url.contains("/digit/")) && 
                   !url.contains("?") &&  // 排除带参数的链接
                   !url.contains("#");    // 排除锚点链接
        }
        
        // 直播吧链接规则
        if (url.contains("zhibo8.com")) {
            return url.contains("/zuqiu/") && 
                   url.matches(".*\\d{4}-\\d{2}-\\d{2}.*") && // 包含日期格式
                   !url.contains("?") && 
                   !url.contains("#");
        }
        
        // 通用规则
        return url.contains("/article/") || 
               url.contains("/news/") || 
               url.contains("/digit/");
    }

    /**
     * 清理已爬取的URL缓存
     */
    public void clearCrawledUrls() {
        int size = crawledUrls.size();
        crawledUrls.clear();
        log.info("Cleared {} URLs from crawler cache", size);
    }
} 