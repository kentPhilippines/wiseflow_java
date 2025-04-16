package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 域名配置实体类
 * 存储网站的TDK（标题、描述、关键词）和友情链接等配置信息
 */
@Data
@TableName("wf_domain_config")
public class DomainConfig {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    /**
     * 域名
     */
    private String domain;
    
    /**
     * 网站标题
     */
    private String title;
    
    /**
     * 网站描述
     */
    private String description;
    
    /**
     * 网站关键词，多个关键词用逗号分隔
     */
    private String keywords;
    
    /**
     * 网站LOGO URL
     */
    private String logoUrl;
    
    /**
     * 网站图标 URL
     */
    private String faviconUrl;
    
    /**
     * 版权信息
     */
    private String copyright;
    
    /**
     * 备案号
     */
    private String icp;
    

    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 模版路径
     */
    private String viewsPath;
    
    /**
     * 联系邮箱
     */
    private String contactEmail;
    
    /**
     * 联系地址
     */
    private String contactAddress;
    
    /**
     * 友情链接，JSON格式存储
     */
    private String friendlyLinks;
    
    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status = 1;
    
    /**
     * 每天新增文章数量
     */
    private Integer dailyAddNewsCount;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 友情链接对象，非数据库字段
     */
    @TableField(exist = false)
    private List<FriendlyLink> friendlyLinkList;
    
    /**
     * 关联的评论规则，非数据库字段
     */
    @TableField(exist = false)
    private List<CommentRule> commentRules;
    
    /**
     * 关联的SEO关键词，非数据库字段
     */
    @TableField(exist = false)
    private List<SeoKeyword> seoKeywords;
    
    /**
     * 友情链接内部类
     */
    @Data
    public static class FriendlyLink {
        /**
         * 链接名称
         */
        private String name;
        
        /**
         * 链接URL
         */
        private String url;
        
        /**
         * 链接描述
         */
        private String description;
        
        /**
         * 排序
         */
        private Integer sort = 0;
    }



    public List<String> getKeywordsSeo(){
        if(keywords == null || keywords.isEmpty()){
            return new ArrayList<>();
        }
        String[] keywordsArray = keywords.split("[,，;；|｜\\s/／、~～@#$%^&*()（）{}【】\\[\\]\\-_=+]+");
        List<String> keywordsSeo = new ArrayList<>();
        for(String keyword : keywordsArray){
            if(!keyword.trim().isEmpty()) {
                keywordsSeo.add(keyword.trim());
            }
        }
        return keywordsSeo;
    }

    
} 