package com.wiseflow.vo;

import lombok.Data;

/**
 * SEO关键词VO类，用于前端表单提交的数据传输
 */
@Data
public class SeoKeywordVO {
    
    /**
     * 主键ID
     */
    private Integer id;
    
    /**
     * 关键词名称
     */
    private String name;
    
    /**
     * 关键词描述
     */
    private String description;
    
    /**
     * 关键词频率 (0-100)
     */
    private Integer frequency;
    
    /**
     * 关键词密度 (0-100)
     */
    private Integer density;
    
    /**
     * 优先级 (1-10)
     */
    private Integer priority;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 关键词分类
     */
    private String category;
    
    /**
     * 关键词位置 (标题、内容、描述等)
     */
    private String position;
    
    /**
     * 单个域名ID
     */
    private Integer domainId;
    
    /**
     * 批量域名IDs（逗号分隔）
     */
    private String domainIds;
    
    /**
     * 联系电话/批量号
     */
    private String phone;
    
    /**
     * 是否覆盖已有配置
     */
    private boolean overwriteExisting;
} 