package com.wiseflow.model;

import lombok.Data;

/**
 * 爬虫配置
 */
@Data
public class CrawlConfig {
    
    /**
     * 爬虫URL
     */
    private String url;
    
    /**
     * 用户代理
     */
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    /**
     * 超时时间（毫秒）
     */
    private int timeout = 10000;
    
    /**
     * 解析规则
     */
    private ParseRule parseRule;
} 