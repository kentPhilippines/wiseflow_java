package com.wiseflow.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 使用Caffeine作为本地缓存实现
 */
@Configuration
public class CacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String CACHE_NEWS_LIST = "newsList";
    public static final String CACHE_NEWS_DETAIL = "newsDetail";
    public static final String CACHE_HOT_NEWS = "hotNews";
    public static final String CACHE_CATEGORY_LIST = "categoryList";
    public static final String CACHE_TAG_LIST = "tagList";
    public static final String CACHE_SEARCH_RESULT = "searchResult";
    public static final String CACHE_CATEGORY_NEWS_COUNT = "categoryNewsCount";
    public static final String CACHE_CRAWLER_LOGS = "crawlerLogs";
    public static final String CACHE_NEWS_DETAIL_API = "newsDetailApi";
    public static final String CACHE_NEWS_DETAIL_ADMIN = "newsDetailAdmin";
    public static final String CACHE_DOMAIN_CONFIG = "domainConfig";
    public static final String CACHE_ARTICLE_TITLE = "articleTitle";
    public static final String CACHE_ARTICLE_CONTENT = "articleContent";
    
    /**
     * 配置缓存管理器
     * 根据缓存名称使用不同的过期时间
     */
    @Primary
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 设置缓存的名称列表
        cacheManager.setCacheNames(Arrays.asList(
                CACHE_NEWS_LIST,
                CACHE_NEWS_DETAIL,
                CACHE_HOT_NEWS,
                CACHE_CATEGORY_LIST,
                CACHE_TAG_LIST,
                CACHE_SEARCH_RESULT,
                CACHE_CATEGORY_NEWS_COUNT,
                CACHE_CRAWLER_LOGS,
                CACHE_NEWS_DETAIL_API,
                CACHE_NEWS_DETAIL_ADMIN,
                CACHE_DOMAIN_CONFIG,
                CACHE_ARTICLE_TITLE,
                CACHE_ARTICLE_CONTENT
        ));
        
        // 使用缓存构建器函数
        cacheManager.setCacheLoader(cacheName -> {
            if (CACHE_ARTICLE_TITLE.equals(cacheName) || CACHE_ARTICLE_CONTENT.equals(cacheName)) {
                // 文章标题和内容使用3小时过期时间
                return Caffeine.newBuilder()
                        .initialCapacity(100)
                        .maximumSize(5000)
                        .expireAfterWrite(3, TimeUnit.HOURS)
                        .recordStats()
                        .build();
            } else {
                // 其他缓存使用5分钟过期时间
                return Caffeine.newBuilder()
                        .initialCapacity(100)
                        .maximumSize(1000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build();
            }
        });
        
        return cacheManager;
    }
} 