package com.wiseflow.model;

import lombok.Data;

/**
 * 网页解析规则
 */
@Data
public class ParseRule {
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 标题选择器
     */
    private String titleSelector = "h1.post_title";
    
    /**
     * 内容选择器
     */
    private String contentSelector = "div.post_body";
    
    /**
     * 作者选择器
     */
    private String authorSelector = "div.post_author";
    
    /**
     * 图片选择器
     */
    private String imageSelector = "div.post_body img";
} 