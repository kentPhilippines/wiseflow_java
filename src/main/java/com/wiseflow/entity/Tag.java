package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@TableName("wf_tag")
public class Tag {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    private String name;
    
    private Integer frequency = 0;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private Set<News> news = new HashSet<>();
    
    // 增加使用频率的方法
    public void incrementFrequency() {
        this.frequency = this.frequency + 1;
    }
} 