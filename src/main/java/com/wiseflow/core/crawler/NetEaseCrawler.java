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
        // 提取发布时间
        LocalDateTime publishTime = extractPublishTime(doc);

        // 提取图片
        List<String> images = extractImages(doc, config);
        if (images.isEmpty()) {
            // 检查发布时间是否在2个月前
            LocalDateTime twoMonthsAgo = LocalDateTime.now().minusMonths(2);
            if (publishTime.isAfter(twoMonthsAgo)) {
                log.warn("未找到图片且发布时间在2个月内，跳过: {}", url);
                return null;
            } else {
                log.info("文章发布时间在2个月前，允许无图片入库: {}", url);
            }
        }
        
        // 提取作者
        String author = extractAuthor(doc, config);
        

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
        newsService.save1(news);
        
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
        // 网易新闻标题通常在 h1 标签中
        Element titleElement = doc.selectFirst("h1");
        return titleElement != null ? titleElement.text().trim() : null;
    }
    
    /**
     * 提取内容
     */
    private String extractContent(Document doc, CrawlConfig config) {
        // 网易新闻正文内容通常在 .post_body 或 .post_content 类中
        Element contentElement = doc.selectFirst(".post_body");
        if (contentElement == null) {
            return null;
        }
        
        // 移除不需要的元素
        contentElement.select("script, style, .gg, .ad, .recommend, .post_recommend, .ep-source-wrapper").remove();
        
        // 处理段落
        Elements paragraphs = contentElement.select("p");
        for (Element p : paragraphs) {
            // 移除空段落
            if (p.text().trim().isEmpty() && !p.hasClass("f_center")) {
                p.remove();
                continue;
            }
            
            // 处理段落样式
            if (!p.hasClass("f_center")) {
                p.attr("style", "margin: 1.5em 0; line-height: 1.75;");
            }
            
            // 移除段落ID和多余属性
            p.removeAttr("id").removeAttr("class").removeAttr("data-role");
        }
        
        // 处理图片和图片描述
        Elements imageContainers = contentElement.select("p.f_center");
        for (Element imgContainer : imageContainers) {
            Element img = imgContainer.selectFirst("img");
            if (img != null) {
                // 处理图片URL
                String src = img.attr("src");
                if (src.contains("?")) {
                    src = src.substring(0, src.indexOf("?"));
                }
                img.attr("src", src);
                
                // 处理图片描述
                String imgDesc = "";
                Element descElement = imgContainer.selectFirst(".desc");
                if (descElement != null) {
                    imgDesc = descElement.text().trim();
                    descElement.remove();
                }
                
                // 重构图片容器
                imgContainer.html("")
                    .attr("style", "text-align: center; margin: 20px 0;")
                    .appendChild(img.attr("style", "max-width: 100%; height: auto;"));
                
                // 添加图片描述
                if (!imgDesc.isEmpty()) {
                    imgContainer.appendElement("div")
                        .attr("style", "color: #666; font-size: 14px; margin-top: 8px; text-align: center;")
                        .text(imgDesc);
                }
            }
        }
        
        // 处理引用块
        Elements quotes = contentElement.select("blockquote");
        for (Element quote : quotes) {
            quote.attr("style", "margin: 1.5em 0; padding: 1em; background-color: #f5f5f5; border-left: 4px solid #ddd;");
        }
        
        // 处理链接
        Elements links = contentElement.select("a");
        for (Element link : links) {
            // 移除搜索相关属性
            link.removeAttr("search-type").removeAttr("search-href");
            // 保留href和文本
            if (!link.hasAttr("href") || link.attr("href").trim().isEmpty()) {
                link.unwrap(); // 如果没有有效链接，只保留文本
            }
        }
        
        // 处理强调文本
        Elements emphases = contentElement.select("em, strong");
        for (Element em : emphases) {
            em.attr("style", "font-weight: bold;");
        }
        
        // 移除多余的换行和空格
        String html = contentElement.html().trim()
            .replaceAll("(?m)^\\s+$", "") // 移除仅包含空白字符的行
            .replaceAll("\\n\\s*\\n", "\n") // 合并多个空行
            .replaceAll("(<br\\s*/?\\s*>\\s*){3,}", "<br><br>"); // 限制连续换行符
            
        return html;
    }
    
    /**
     * 提取作者
     */
    private String extractAuthor(Document doc, CrawlConfig config) {
        // 从 post_author 中提取作者信息
        Element postAuthor = doc.selectFirst(".post_author");
        if (postAuthor != null) {
            String authorText = postAuthor.text();
            
            // 尝试提取作者信息
            Pattern authorPattern = Pattern.compile("作者[:|：]\\s*([^\\s]+)");
            Matcher authorMatcher = authorPattern.matcher(authorText);
            if (authorMatcher.find()) {
                return authorMatcher.group(1).trim();
            }
            
            // 尝试提取责任编辑
            Pattern editorPattern = Pattern.compile("责任编辑[:|：]\\s*([^\\s]+)");
            Matcher editorMatcher = editorPattern.matcher(authorText);
            if (editorMatcher.find()) {
                String editor = editorMatcher.group(1).trim();
                // 移除编辑ID后缀（如 _NS1098）
                return editor.replaceAll("_[A-Z0-9]+$", "");
            }
            
            // 尝试提取来源
            Pattern sourcePattern = Pattern.compile("本文来源[:|：]\\s*([^\\s]+)");
            Matcher sourceMatcher = sourcePattern.matcher(authorText);
            if (sourceMatcher.find()) {
                return sourceMatcher.group(1).trim();
            }
        }
        
        // 从 post_info 中提取来源信息作为备选
        Element postInfo = doc.selectFirst(".post_info");
        if (postInfo != null) {
            String infoText = postInfo.ownText();
            Pattern sourcePattern = Pattern.compile("来源[:|：]\\s*([^\\s]+)");
            Matcher matcher = sourcePattern.matcher(infoText);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        
        // 其他备用选择器
        String[] backupSelectors = {".ep-source"};
        for (String selector : backupSelectors) {
            Element authorElement = doc.selectFirst(selector);
            if (authorElement != null) {
                String author = authorElement.text().trim();
                author = author.replaceAll("^(来源[:|：]|作者[:|：]|本文来源[:|：])\\s*", "");
                if (!author.isEmpty()) {
                    return author;
                }
            }
        }
        
        return "网易新闻";
    }
    
    /**
     * 提取发布时间
     */
    private LocalDateTime extractPublishTime(Document doc) {
        // 从 post_info 中提取时间
        Element postInfo = doc.selectFirst(".post_info");
        if (postInfo != null) {
            String infoText = postInfo.ownText();  // 获取不包含子元素的文本
            Matcher matcher = DATE_PATTERN.matcher(infoText);
            if (matcher.find()) {
                try {
                    return LocalDateTime.parse(matcher.group(1), 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e) {
                    log.debug("解析时间失败: {}", infoText);
                }
            }
        }
        
        // 备用时间选择器
        Elements timeElements = doc.select(".pub-time, .post_time");
        for (Element element : timeElements) {
            String timeText = element.text().trim();
            timeText = timeText.replaceAll("^(发布时间[:|：]|更新时间[:|：])\\s*", "");
            
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
        
        return LocalDateTime.now();
    }
    
    /**
     * 提取图片
     */
    private List<String> extractImages(Document doc, CrawlConfig config) {
        List<String> images = new ArrayList<>();
        Set<String> uniqueImages = new HashSet<>();  // 用于去重
        
        // 处理正文中的图片
        Elements imageContainers = doc.select(".post_body p.f_center, .post_content p.f_center");
        for (Element container : imageContainers) {
            Element img = container.selectFirst("img");
            if (img != null) {
                String imgUrl = processImageUrl(img.attr("src"));
                if (imgUrl != null && uniqueImages.add(imgUrl)) {  // 确保不重复添加
                    images.add(imgUrl);
                }
            }
        }
        
        // 如果正文中没有找到图片，尝试其他位置
        if (images.isEmpty()) {
            Elements allImages = doc.select(".post_body img, .post_content img");
            for (Element img : allImages) {
                // 优先获取原图URL
                String src = img.hasAttr("data-src") ? img.attr("data-src") : img.attr("src");
                String imgUrl = processImageUrl(src);
                if (imgUrl != null && uniqueImages.add(imgUrl)) {
                    images.add(imgUrl);
                }
            }
        }
        
        return images;
    }
    
    /**
     * 处理图片URL
     */
    private String processImageUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        
        // 处理相对路径
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        
        // 解析URL中的实际图片地址
        if (url.contains("?url=")) {
            try {
                // 提取实际图片URL
                String encodedUrl = url.substring(url.indexOf("?url=") + 5);
                if (encodedUrl.contains("&")) {
                    encodedUrl = encodedUrl.substring(0, encodedUrl.indexOf("&"));
                }
                // URL解码
                url = java.net.URLDecoder.decode(encodedUrl, "UTF-8");
            } catch (Exception e) {
                log.warn("解析图片URL失败: {}", url);
                return null;
            }
        }
        
        // 过滤条件
        if (url.contains("icon") || url.contains("logo") || 
            url.contains("avatar") || !url.contains("126.net")) {
            return null;
        }
        
        return url;
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