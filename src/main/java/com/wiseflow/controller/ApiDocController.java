package com.wiseflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * API文档控制器
 * 提供API调用的请求响应参数案例，便于前端调试
 */
@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
public class ApiDocController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 获取所有API接口文档
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllApiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        
        Map<String, Object> data = new HashMap<>();
        data.put("news", generateNewsApiDocs());
        data.put("category", generateCategoryApiDocs());
        data.put("tag", generateTagApiDocs());
        data.put("config", generateConfigApiDocs());
        
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取新闻相关API文档
     */
    @GetMapping("/news")
    public ResponseEntity<Map<String, Object>> getNewsApiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", generateNewsApiDocs());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取分类相关API文档
     */
    @GetMapping("/category")
    public ResponseEntity<Map<String, Object>> getCategoryApiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", generateCategoryApiDocs());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取标签相关API文档
     */
    @GetMapping("/tag")
    public ResponseEntity<Map<String, Object>> getTagApiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", generateTagApiDocs());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取配置相关API文档
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfigApiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", generateConfigApiDocs());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 新闻相关API文档
     */
    private Map<String, Object> generateNewsApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        
        // 新闻列表API
        Map<String, Object> newsList = new HashMap<>();
        newsList.put("url", "/api/news/list");
        newsList.put("method", "GET");
        newsList.put("description", "获取新闻列表，默认按照时间和热度交叉排序");
        
        Map<String, Object> newsListParams = new HashMap<>();
        newsListParams.put("categoryName", "分类名称，可选");
        newsListParams.put("pageNum", "页码，默认1");
        newsListParams.put("pageSize", "每页大小，默认10");
        newsListParams.put("all", "是否获取全部新闻，默认false。为true时将按时间排序返回所有新闻，忽略其他排序规则");
        newsList.put("params", newsListParams);
        
        Map<String, Object> newsListResponse = new HashMap<>();
        newsListResponse.put("code", 200);
        newsListResponse.put("message", "success");
        
        Map<String, Object> newsListData = new HashMap<>();
        List<Map<String, Object>> newsListItems = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> newsItem = new HashMap<>();
            newsItem.put("id", i);
            newsItem.put("title", "新闻标题" + i);
            newsItem.put("categoryName", "国内");
            newsItem.put("publishTime", LocalDateTime.now().minusDays(i).format(DATE_FORMATTER));
            newsItem.put("viewCount", 100 + i * 10);
            newsItem.put("coverImage", "https://example.com/images/news" + i + ".jpg");
            newsItem.put("summary", "这是新闻" + i + "的摘要，简要介绍了新闻的主要内容...");
            newsListItems.add(newsItem);
        }
        
        newsListData.put("list", newsListItems);
        newsListData.put("total", 28);
        newsListData.put("pages", 3);
        
        newsListResponse.put("data", newsListData);
        newsList.put("response", newsListResponse);
        
        // 新闻详情API
        Map<String, Object> newsDetail = new HashMap<>();
        newsDetail.put("url", "/api/news/detail/{id}");
        newsDetail.put("method", "GET");
        newsDetail.put("description", "获取新闻详情");
        
        Map<String, Object> newsDetailParams = new HashMap<>();
        newsDetailParams.put("id", "新闻ID，必填");
        newsDetail.put("params", newsDetailParams);
        
        Map<String, Object> newsDetailResponse = new HashMap<>();
        newsDetailResponse.put("code", 200);
        newsDetailResponse.put("message", "success");
        
        Map<String, Object> newsDetailData = new HashMap<>();
        newsDetailData.put("id", 1);
        newsDetailData.put("title", "新闻标题1");
        newsDetailData.put("subtitle", "新闻副标题1");
        newsDetailData.put("categoryId", 1);
        newsDetailData.put("categoryName", "国内");
        newsDetailData.put("author", "记者张三");
        newsDetailData.put("source", "网易新闻");
        newsDetailData.put("url", "https://news.163.com/article/123456.html");
        newsDetailData.put("publishTime", LocalDateTime.now().minusDays(1).format(DATE_FORMATTER));
        newsDetailData.put("viewCount", 110);
        newsDetailData.put("commentCount", 15);
        newsDetailData.put("likeCount", 25);
        
        newsDetailData.put("content", "这是新闻1的正文内容，详细介绍了新闻的全部内容...");
        newsDetailData.put("contentHtml", "<p>这是新闻1的正文内容，详细介绍了新闻的全部内容...</p>");
        newsDetailData.put("summary", "这是新闻1的摘要，简要介绍了新闻的主要内容...");
        
        List<Map<String, Object>> images = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> image = new HashMap<>();
            image.put("url", "https://example.com/images/news1-" + i + ".jpg");
            image.put("position", i - 1);
            image.put("isCover", i == 1);
            images.add(image);
        }
        newsDetailData.put("images", images);
        
        List<Map<String, Object>> tags = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> tag = new HashMap<>();
            tag.put("id", i);
            tag.put("name", "标签" + i);
            tags.add(tag);
        }
        newsDetailData.put("tags", tags);
        
        newsDetailResponse.put("data", newsDetailData);
        newsDetail.put("response", newsDetailResponse);
        
        // 热门新闻API
        Map<String, Object> hotNews = new HashMap<>();
        hotNews.put("url", "/api/news/hot");
        hotNews.put("method", "GET");
        hotNews.put("description", "获取热门新闻");
        
        Map<String, Object> hotNewsParams = new HashMap<>();
        hotNewsParams.put("limit", "限制数量，默认5");
        hotNewsParams.put("all", "是否获取全部热门新闻，默认false。为true时将返回所有热门新闻，忽略limit参数");
        hotNews.put("params", hotNewsParams);
        
        Map<String, Object> hotNewsResponse = new HashMap<>();
        hotNewsResponse.put("code", 200);
        hotNewsResponse.put("message", "success");
        
        List<Map<String, Object>> hotNewsItems = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> newsItem = new HashMap<>();
            newsItem.put("id", i);
            newsItem.put("title", "热门新闻标题" + i);
            newsItem.put("categoryName", i % 2 == 0 ? "国际" : "国内");
            newsItem.put("publishTime", LocalDateTime.now().minusDays(i).format(DATE_FORMATTER));
            newsItem.put("viewCount", 500 + i * 100);
            newsItem.put("coverImage", "https://example.com/images/hot" + i + ".jpg");
            newsItem.put("summary", "这是热门新闻" + i + "的摘要，简要介绍了新闻的主要内容...");
            hotNewsItems.add(newsItem);
        }
        
        hotNewsResponse.put("data", hotNewsItems);
        hotNews.put("response", hotNewsResponse);
        
        // 搜索新闻API
        Map<String, Object> searchNews = new HashMap<>();
        searchNews.put("url", "/api/news/search");
        searchNews.put("method", "GET");
        searchNews.put("description", "搜索新闻");
        
        Map<String, Object> searchNewsParams = new HashMap<>();
        searchNewsParams.put("keyword", "搜索关键词，必填");
        searchNewsParams.put("pageNum", "页码，默认1");
        searchNewsParams.put("pageSize", "每页大小，默认10");
        searchNewsParams.put("all", "是否获取全部搜索结果，默认false。为true时将返回所有匹配的新闻，忽略分页");
        searchNews.put("params", searchNewsParams);
        
        Map<String, Object> searchNewsResponse = new HashMap<>();
        searchNewsResponse.put("code", 200);
        searchNewsResponse.put("message", "success");
        
        Map<String, Object> searchNewsData = new HashMap<>();
        List<Map<String, Object>> searchNewsItems = new ArrayList<>();
        
        for (int i = 1; i <= 2; i++) {
            Map<String, Object> newsItem = new HashMap<>();
            newsItem.put("id", i);
            newsItem.put("title", "搜索结果标题" + i);
            newsItem.put("categoryName", i % 2 == 0 ? "科技" : "财经");
            newsItem.put("publishTime", LocalDateTime.now().minusDays(i).format(DATE_FORMATTER));
            newsItem.put("viewCount", 200 + i * 50);
            newsItem.put("coverImage", "https://example.com/images/search" + i + ".jpg");
            newsItem.put("summary", "这是搜索结果" + i + "的摘要，包含了搜索关键词...");
            searchNewsItems.add(newsItem);
        }
        
        searchNewsData.put("list", searchNewsItems);
        searchNewsData.put("total", 2);
        searchNewsData.put("pages", 1);
        
        searchNewsResponse.put("data", searchNewsData);
        searchNews.put("response", searchNewsResponse);
        
        // 修改API文档：分页查询新闻（按时间和热度交叉排序）
        Map<String, Object> allNews = new HashMap<>();
        allNews.put("url", "/api/news");
        allNews.put("method", "GET");
        allNews.put("description", "分页查询新闻（按时间和热度交叉排序）");
        
        Map<String, Object> allNewsParams = new HashMap<>();
        allNewsParams.put("page", "页码，默认1");
        allNewsParams.put("size", "每页大小，默认10");
        allNewsParams.put("keyword", "搜索关键词，可选");
        allNewsParams.put("categoryId", "分类ID，可选");
        allNews.put("params", allNewsParams);
        
        Map<String, Object> allNewsResponse = new HashMap<>();
        allNewsResponse.put("records", Arrays.asList(
            Map.of("id", 1, "title", "热门新闻标题1", "categoryName", "国内", "viewCount", 500, "publishTime", LocalDateTime.now().minusHours(2).format(DATE_FORMATTER)),
            Map.of("id", 2, "title", "热门新闻标题2", "categoryName", "国际", "viewCount", 450, "publishTime", LocalDateTime.now().minusHours(5).format(DATE_FORMATTER))
        ));
        allNewsResponse.put("total", 28);
        allNewsResponse.put("size", 10);
        allNewsResponse.put("current", 1);
        allNewsResponse.put("pages", 3);
        
        allNews.put("response", allNewsResponse);
        
        docs.put("newsList", newsList);
        docs.put("newsDetail", newsDetail);
        docs.put("hotNews", hotNews);
        docs.put("searchNews", searchNews);
        docs.put("allNews", allNews);
        
        return docs;
    }
    
    /**
     * 分类相关API文档
     */
    private Map<String, Object> generateCategoryApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        
        // 分类列表API
        Map<String, Object> categoryList = new HashMap<>();
        categoryList.put("url", "/api/category/list");
        categoryList.put("method", "GET");
        categoryList.put("description", "获取分类列表及其新闻数量");
        
        Map<String, Object> categoryListResponse = new HashMap<>();
        categoryListResponse.put("code", 200);
        categoryListResponse.put("message", "success");
        
        List<Map<String, Object>> categories = new ArrayList<>();
        String[] categoryNames = {"国内", "国际", "财经", "科技", "体育", "娱乐"};
        
        for (int i = 0; i < categoryNames.length; i++) {
            Map<String, Object> categoryItem = new HashMap<>();
            
            Map<String, Object> category = new HashMap<>();
            category.put("id", i + 1);
            category.put("name", categoryNames[i]);
            category.put("code", "category_" + (i + 1));
            category.put("level", 1);
            category.put("parentId", null);
            category.put("sort", i);
            category.put("description", categoryNames[i] + "新闻分类");
            category.put("status", 1);
            
            categoryItem.put("category", category);
            categoryItem.put("newsCount", 10 + i * 5);
            
            categories.add(categoryItem);
        }
        
        categoryListResponse.put("data", categories);
        categoryList.put("response", categoryListResponse);
        
        // 获取指定分类的新闻数量API
        Map<String, Object> categoryNewsCount = new HashMap<>();
        categoryNewsCount.put("url", "/api/category/{id}/count");
        categoryNewsCount.put("method", "GET");
        categoryNewsCount.put("description", "获取指定分类的新闻数量");
        
        Map<String, Object> categoryNewsCountParams = new HashMap<>();
        categoryNewsCountParams.put("id", "分类ID，必填");
        categoryNewsCount.put("params", categoryNewsCountParams);
        
        Map<String, Object> categoryNewsCountResponse = new HashMap<>();
        categoryNewsCountResponse.put("code", 200);
        categoryNewsCountResponse.put("message", "success");
        categoryNewsCountResponse.put("data", 25);
        
        categoryNewsCount.put("response", categoryNewsCountResponse);
        
        // 添加新的API文档：获取所有分类
        Map<String, Object> allCategories = new HashMap<>();
        allCategories.put("url", "/api/categories");
        allCategories.put("method", "GET");
        allCategories.put("description", "获取所有分类");
        
        List<Map<String, Object>> allCategoriesResponse = new ArrayList<>();
        for (int i = 0; i < categoryNames.length; i++) {
            Map<String, Object> category = new HashMap<>();
            category.put("id", i + 1);
            category.put("name", categoryNames[i]);
            category.put("code", "category_" + (i + 1));
            category.put("level", 1);
            category.put("parentId", null);
            category.put("sort", i);
            category.put("description", categoryNames[i] + "新闻分类");
            category.put("status", 1);
            allCategoriesResponse.add(category);
        }
        
        allCategories.put("response", allCategoriesResponse);
        
        docs.put("categoryList", categoryList);
        docs.put("categoryNewsCount", categoryNewsCount);
        docs.put("allCategories", allCategories);
        
        return docs;
    }
    
    /**
     * 标签相关API文档
     */
    private Map<String, Object> generateTagApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        
        // 标签列表API
        Map<String, Object> tagList = new HashMap<>();
        tagList.put("url", "/api/tag/list");
        tagList.put("method", "GET");
        tagList.put("description", "获取标签列表");
        
        Map<String, Object> tagListParams = new HashMap<>();
        tagListParams.put("limit", "限制数量，默认20");
        tagList.put("params", tagListParams);
        
        Map<String, Object> tagListResponse = new HashMap<>();
        tagListResponse.put("code", 200);
        tagListResponse.put("message", "success");
        
        List<Map<String, Object>> tags = new ArrayList<>();
        String[] tagNames = {"政治", "经济", "军事", "文化", "社会", "教育", "科技", "健康", "体育", "娱乐"};
        
        for (int i = 0; i < tagNames.length; i++) {
            Map<String, Object> tag = new HashMap<>();
            tag.put("id", i + 1);
            tag.put("name", tagNames[i]);
            tag.put("frequency", 50 - i * 3);
            tags.add(tag);
        }
        
        tagListResponse.put("data", tags);
        tagList.put("response", tagListResponse);
        
        // 添加新的API文档：分页查询所有标签
        Map<String, Object> allTags = new HashMap<>();
        allTags.put("url", "/api/tags");
        allTags.put("method", "GET");
        allTags.put("description", "分页查询所有标签");
        
        Map<String, Object> allTagsParams = new HashMap<>();
        allTagsParams.put("page", "页码，默认0");
        allTagsParams.put("size", "每页大小，默认10");
        allTags.put("params", allTagsParams);
        
        Map<String, Object> allTagsResponse = new HashMap<>();
        allTagsResponse.put("records", Arrays.asList(
            Map.of("id", 1, "name", "政治", "frequency", 50),
            Map.of("id", 2, "name", "经济", "frequency", 47)
        ));
        allTagsResponse.put("total", 10);
        allTagsResponse.put("size", 10);
        allTagsResponse.put("current", 1);
        allTagsResponse.put("pages", 1);
        
        allTags.put("response", allTagsResponse);
        
        docs.put("tagList", tagList);
        docs.put("allTags", allTags);
        
        return docs;
    }
    
    /**
     * 爬虫相关API文档
     */
    @GetMapping("/crawler")
    public ResponseEntity<Map<String, Object>> getCrawlerApiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", generateCrawlerApiDocs());
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> generateCrawlerApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        
        // 启动爬虫API
        Map<String, Object> startCrawler = new HashMap<>();
        startCrawler.put("url", "/api/crawler/start/{spiderName}");
        startCrawler.put("method", "POST");
        startCrawler.put("description", "启动爬虫");
        
        Map<String, Object> startCrawlerParams = new HashMap<>();
        startCrawlerParams.put("spiderName", "爬虫名称，必填");
        startCrawler.put("params", startCrawlerParams);
        
        Map<String, Object> startCrawlerResponse = new HashMap<>();
        startCrawlerResponse.put("code", 200);
        startCrawlerResponse.put("message", "success");
        
        startCrawler.put("response", startCrawlerResponse);
        
        // 停止爬虫API
        Map<String, Object> stopCrawler = new HashMap<>();
        stopCrawler.put("url", "/api/crawler/stop/{spiderName}");
        stopCrawler.put("method", "POST");
        stopCrawler.put("description", "停止爬虫");
        
        Map<String, Object> stopCrawlerParams = new HashMap<>();
        stopCrawlerParams.put("spiderName", "爬虫名称，必填");
        stopCrawler.put("params", stopCrawlerParams);
        
        Map<String, Object> stopCrawlerResponse = new HashMap<>();
        stopCrawlerResponse.put("code", 200);
        stopCrawlerResponse.put("message", "success");
        
        stopCrawler.put("response", stopCrawlerResponse);
        
        // 获取爬虫日志API
        Map<String, Object> crawlerLogs = new HashMap<>();
        crawlerLogs.put("url", "/api/crawler/logs");
        crawlerLogs.put("method", "GET");
        crawlerLogs.put("description", "获取爬虫日志");
        
        Map<String, Object> crawlerLogsParams = new HashMap<>();
        crawlerLogsParams.put("page", "页码，默认0");
        crawlerLogsParams.put("size", "每页大小，默认10");
        crawlerLogs.put("params", crawlerLogsParams);
        
        Map<String, Object> crawlerLogsResponse = new HashMap<>();
        crawlerLogsResponse.put("records", Arrays.asList(
            Map.of(
                "id", 1, 
                "spiderName", "netease", 
                "startTime", LocalDateTime.now().minusHours(2).format(DATE_FORMATTER),
                "endTime", LocalDateTime.now().minusHours(1).format(DATE_FORMATTER),
                "duration", 3600.0,
                "status", 1,
                "urlCount", 100,
                "successCount", 95,
                "failCount", 5
            ),
            Map.of(
                "id", 2, 
                "spiderName", "sina", 
                "startTime", LocalDateTime.now().minusHours(4).format(DATE_FORMATTER),
                "endTime", LocalDateTime.now().minusHours(3).format(DATE_FORMATTER),
                "duration", 3600.0,
                "status", 1,
                "urlCount", 80,
                "successCount", 75,
                "failCount", 5
            )
        ));
        crawlerLogsResponse.put("total", 10);
        crawlerLogsResponse.put("size", 10);
        crawlerLogsResponse.put("current", 1);
        crawlerLogsResponse.put("pages", 1);
        
        crawlerLogs.put("response", crawlerLogsResponse);
        
        docs.put("startCrawler", startCrawler);
        docs.put("stopCrawler", stopCrawler);
        docs.put("crawlerLogs", crawlerLogs);
        
        return docs;
    }
    
    /**
     * 配置相关API文档
     */
    private Map<String, Object> generateConfigApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        
        // 域名配置API
        Map<String, Object> domainConfig = new HashMap<>();
        domainConfig.put("url", "/api/config/domain/{domain}");
        domainConfig.put("method", "GET");
        domainConfig.put("description", "获取域名配置，包括网站标题、描述、关键词和友情链接等");
        
        Map<String, Object> domainConfigParams = new HashMap<>();
        domainConfigParams.put("domain", "域名，必填");
        domainConfig.put("params", domainConfigParams);
        
        Map<String, Object> domainConfigResponse = new HashMap<>();
        domainConfigResponse.put("code", 200);
        domainConfigResponse.put("message", "success");
        
        Map<String, Object> domainConfigData = new HashMap<>();
        domainConfigData.put("title", "WiseFlow新闻爬虫系统");
        domainConfigData.put("description", "一个基于Spring Boot和MyBatis-Plus的新闻爬虫系统");
        domainConfigData.put("keywords", "新闻,爬虫,Spring Boot,MyBatis-Plus");
        domainConfigData.put("logoUrl", "https://example.com/logo.png");
        domainConfigData.put("faviconUrl", "https://example.com/favicon.ico");
        domainConfigData.put("copyright", "© 2023 WiseFlow. All rights reserved.");
        domainConfigData.put("icp", "京ICP备12345678号");
        domainConfigData.put("contactPhone", "010-12345678");
        domainConfigData.put("contactEmail", "contact@wiseflow.com");
        domainConfigData.put("contactAddress", "北京市海淀区中关村");
        domainConfigData.put("viewsPath", "templates/views1");

        List<Map<String, Object>> friendlyLinks = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> link = new HashMap<>();
            link.put("name", "友情链接" + i);
            link.put("url", "https://example" + i + ".com");
            link.put("description", "这是友情链接" + i + "的描述");
            link.put("sort", i - 1);
            friendlyLinks.add(link);
        }
        domainConfigData.put("friendlyLinks", friendlyLinks);
        
        domainConfigResponse.put("data", domainConfigData);
        domainConfig.put("response", domainConfigResponse);
        
        docs.put("domainConfig", domainConfig);
        
        return docs;
    }
} 