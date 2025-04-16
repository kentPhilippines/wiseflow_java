package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_activity")
public class Activity {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String type;
    
    private String description;
    
    private String operator;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
} 