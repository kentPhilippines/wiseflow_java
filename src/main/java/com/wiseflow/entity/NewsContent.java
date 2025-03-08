package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("wf_news_content")
public class NewsContent {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    @TableField(exist = false)
    private News news;
    
    private Integer newsId;
    
    private String content;
    
    private String contentHtml;
    
    private String summary;
    
    private String keywords;
} 