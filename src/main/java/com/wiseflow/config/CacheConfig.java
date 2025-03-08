package com.wiseflow.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    
    /**
     * 配置缓存管理器
     * 设置默认过期时间为5分钟
     */
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
                CACHE_DOMAIN_CONFIG
        ));
        
        // 设置缓存配置
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量，超过后会自动清除最近最少使用的缓存
                .maximumSize(1000)
                // 过期时间，最后一次写入后5分钟过期
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 统计缓存命中率
                .recordStats());
        
        return cacheManager;
    }
} 