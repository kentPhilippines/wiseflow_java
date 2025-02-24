package com.wiseflow.model;

import lombok.Data;

@Data
public class CrawlConfig {
    private String name;
    private String url;
    private ParseRule parseRule;
    private boolean enabled;
    private String encoding;
    private int timeout;
    private String userAgent;
} 