package com.wiseflow.core.crawler;

import com.wiseflow.entity.Category;
import com.wiseflow.entity.News;
import com.wiseflow.entity.NewsContent;
import com.wiseflow.entity.NewsImage;
import com.wiseflow.model.CrawlConfig;
import com.wiseflow.service.CategoryService;
import com.wiseflow.service.NewsService;
import com.wiseflow.mapper.NewsContentMapper;
import com.wiseflow.mapper.NewsImageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class NetEaseCrawler {
    
    private final NewsService newsService;
    private final CategoryService categoryService;
    private final NewsContentMapper contentMapper;
    private final NewsImageMapper imageMapper;
    
    private final Set<String> crawledUrls = new HashSet<>();
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})");
    
    /**
     * 爬取网易新闻
     * @param config 爬虫配置
     * @return 爬取的新闻数量
     */
    public int crawl(CrawlConfig config) {
        int count = 0;
        try {
            log.info("开始爬取网易新闻: {}", config.getUrl());
            
            // 获取分类
            Category category = getOrCreateCategory(config.getParseRule().getCategoryName());
            
            // 获取新闻列表页
            Document listDoc = Jsoup.connect(config.getUrl())
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeout())
                    .get();
            
            // 提取新闻链接
            List<String> newsUrls = extractNewsUrls(listDoc);
            log.info("提取到 {} 个新闻链接", newsUrls.size());
            
            // 爬取每个新闻详情
            for (String newsUrl : newsUrls) {
                if (crawledUrls.contains(newsUrl) || newsService.isUrlExists(newsUrl)) {
                    log.debug("跳过已爬取的URL: {}", newsUrl);
                    continue;
                }
                
                try {
                    News news = crawlNewsDetail(newsUrl, config, category);
                    if (news != null) {
                        count++;
                        crawledUrls.add(newsUrl);
                    }
                } catch (Exception e) {
                    log.error("爬取新闻详情失败: {}", newsUrl, e);
                }
            }
            
            log.info("网易新闻爬取完成，成功爬取 {} 条新闻", count);
        } catch (Exception e) {
            log.error("网易新闻爬取失败", e);
        }
        
        return count;
    }
    
    /**
     * 提取新闻链接
     */
    private List<String> extractNewsUrls(Document doc) {
        List<String> urls = new ArrayList<>();
        
        // 提取新闻链接
        Elements newsLinks = doc.select("a[href*=/article/], a[href*=/news/]");
        for (Element link : newsLinks) {
            String url = link.absUrl("href");
            if (isValidNewsUrl(url)) {
                urls.add(url);
            }
        }
        
        return urls;
    }
    
    /**
     * 判断是否为有效的新闻URL
     */
    private boolean isValidNewsUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        
        // 网易新闻URL格式
        return url.contains("163.com") && 
               (url.contains("/article/") || url.contains("/news/")) &&
               !url.contains("?") && 
               !url.contains("#");
    }
    
    /**
     * 爬取新闻详情
     */
    private News crawlNewsDetail(String url, CrawlConfig config, Category category) throws IOException {
        log.info("爬取新闻详情: {}", url);
        
        Document doc = Jsoup.connect(url)
                .userAgent(config.getUserAgent())
                .timeout(config.getTimeout())
                .get();
        
        // 提取标题
        String title = extractTitle(doc, config);
        if (StringUtils.isBlank(title)) {
            log.warn("未找到标题，跳过: {}", url);
            return null;
        }
        
        // 提取内容
        String content = extractContent(doc, config);
        if (StringUtils.isBlank(content)) {
            log.warn("未找到内容，跳过: {}", url);
            return null;
        }
        
        // 提取图片
        List<String> images = extractImages(doc, config);
        if (images.isEmpty()) {
            log.warn("未找到图片，跳过: {}", url);
            return null;
        }
        
        // 提取作者
        String author = extractAuthor(doc, config);
        
        // 提取发布时间
        LocalDateTime publishTime = extractPublishTime(doc);
        
        // 创建并保存新闻对象
        News news = new News();
        news.setTitle(title);
        news.setUrl(url);
        news.setAuthor(author);
        news.setCategory(category);
        news.setCategoryName(category.getName());
        news.setPublishTime(publishTime);
        news.setCrawlTime(LocalDateTime.now());
        news.setCategoryId(category.getId());
        
        // 保存新闻主体
        newsService.save(news);
        
        // 创建并保存新闻内容
        NewsContent newsContent = new NewsContent();
        newsContent.setContent(content);
        newsContent.setContentHtml(content);
        newsContent.setSummary(generateSummary(content));
        newsContent.setNewsId(news.getId());
        contentMapper.insert(newsContent);
        
        // 保存图片
        for (int i = 0; i < images.size(); i++) {
            String imageUrl = images.get(i);
            NewsImage newsImage = new NewsImage();
            newsImage.setUrl(imageUrl);
            newsImage.setNewsId(news.getId());
            newsImage.setPosition(i);
            newsImage.setIsCover(i == 0); // 第一张图片作为封面
            imageMapper.insert(newsImage);
        }
        
        return news;
    }
    
    /**
     * 提取标题
     */
    private String extractTitle(Document doc, CrawlConfig config) {
        Element titleElement = doc.selectFirst(config.getParseRule().getTitleSelector());
        return titleElement != null ? titleElement.text().trim() : null;
    }
    
    /**
     * 提取内容
     */
    private String extractContent(Document doc, CrawlConfig config) {
        Element contentElement = doc.selectFirst(config.getParseRule().getContentSelector());
        if (contentElement == null) {
            return null;
        }
        
        // 移除不需要的元素
        contentElement.select("script, style, .gg, .ad, .recommend").remove();
        
        return contentElement.html().trim();
    }
    
    /**
     * 提取作者
     */
    private String extractAuthor(Document doc, CrawlConfig config) {
        Element authorElement = doc.selectFirst(config.getParseRule().getAuthorSelector());
        return authorElement != null ? authorElement.text().trim() : "网易新闻";
    }
    
    /**
     * 提取发布时间
     */
    private LocalDateTime extractPublishTime(Document doc) {
        // 尝试从多个可能的元素中提取时间
        Elements timeElements = doc.select(".post_time, .time, .date, .publish_time");
        
        for (Element element : timeElements) {
            String timeText = element.text().trim();
            Matcher matcher = DATE_PATTERN.matcher(timeText);
            if (matcher.find()) {
                try {
                    return LocalDateTime.parse(matcher.group(1), 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e) {
                    log.debug("解析时间失败: {}", timeText);
                }
            }
        }
        
        // 如果没有找到时间，返回当前时间
        return LocalDateTime.now();
    }
    
    /**
     * 提取图片
     */
    private List<String> extractImages(Document doc, CrawlConfig config) {
        List<String> images = new ArrayList<>();
        Elements imageElements = doc.select(config.getParseRule().getImageSelector());
        
        for (Element img : imageElements) {
            String src = img.absUrl("src");
            if (StringUtils.isNotBlank(src)) {
                // 处理相对路径
                if (src.startsWith("//")) {
                    src = "https:" + src;
                }
                images.add(src);
            }
        }
        
        return images;
    }
    
    /**
     * 生成摘要
     */
    private String generateSummary(String content) {
        // 简单实现：取前100个字符作为摘要
        String plainText = Jsoup.parse(content).text();
        int length = Math.min(plainText.length(), 100);
        return plainText.substring(0, length) + (plainText.length() > 100 ? "..." : "");
    }
    
    /**
     * 获取或创建分类
     */
    private Category getOrCreateCategory(String categoryName) {
        return categoryService.findByName(categoryName)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(categoryName);
                    category.setDescription("自动创建的分类");
                    return categoryService.save(category);
                });
    }
    
    /**
     * 清理已爬取的URL缓存
     */
    public void clearCrawledUrls() {
        int size = crawledUrls.size();
        crawledUrls.clear();
        log.info("已清理 {} 个已爬取的URL", size);
    }
} 