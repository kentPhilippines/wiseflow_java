package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@TableName("wf_news")
public class News {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    private String title;
    
    private String subtitle;
    
    private String source;
    
    /**
     * 作者
     */
    private String author;
    
    private String url;
    
    @TableField(exist = false)
    private Category category;
    
    private Integer categoryId;
    
    private LocalDateTime publishTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime crawlTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    private Boolean isTop = false;
    
    private Boolean isHot = false;
    
    private Boolean isRecommend = false;
    
    private String domainConfig;

    /**
     * 是否需要AI改写
     */
    private Boolean needRewrite = false;
    /**
     * 改写状态：
     * PENDING - 待处理
     * PROCESSING - 处理中 
     * COMPLETED - 已完成
     * FAILED - 失败
     */
    private String rewriteStatus;
    /**
     * 改写后的新闻ID
     */
    private Integer rewrittenNewsId;
    
    private Integer viewCount = 0;
    
    private Integer commentCount = 0;
    
    private Integer likeCount = 0;
    
    private Integer status = 1;
    
    @TableField(exist = false)
    private NewsContent content;
    
    @TableField(exist = false)
    private List<NewsImage> images = new ArrayList<>();
    
    @TableField(exist = false)
    private Set<Tag> tags = new HashSet<>();
    
    private String categoryName;
    
    @TableField(exist = false)
    private String coverImage;
    

    /**
     * 是否允许评论
     */
    private Boolean isComment = false;
    
    // -- SQL语句:
    // -- ALTER TABLE wf_news ADD COLUMN is_comment TINYINT(1) DEFAULT 0 COMMENT '是否允许评论';

    
    // 添加图片的便捷方法
    public void addImage(NewsImage image) {
        images.add(image);
        image.setNews(this);
    }
    
    // 添加标签的便捷方法
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getNews().add(this);
    }
    
    // 移除标签的便捷方法
    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getNews().remove(this);
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
} 