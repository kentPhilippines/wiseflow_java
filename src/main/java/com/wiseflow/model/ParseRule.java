package com.wiseflow.model;

import lombok.Data;

@Data
public class ParseRule {
    private String titleSelector;
    private String contentSelector;
    private String authorSelector;
    private String dateSelector;
    private String imageSelector;
    private String summarySelector;
    private String urlPattern;
    private String articleLinkSelector;
    private String categoryName;
    private String categorySelector;
} 