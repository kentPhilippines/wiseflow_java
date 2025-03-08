package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_news_image")
public class NewsImage {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    @TableField(exist = false)
    private News news;
    
    private Integer newsId;
    
    private String url;
    
    private String localPath;
    
    private String title;
    
    private String description;
    
    private Integer width;
    
    private Integer height;
    
    private Integer size;
    
    private String format;
    
    private Boolean isCover = false;
    
    private Integer position = 0;
    
    private Integer status = 1;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
} 