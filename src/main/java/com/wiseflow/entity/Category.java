package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("wf_category")
public class Category {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    private String name;
    
    private String code;
    
    private String url;
    
    @TableField(exist = false)
    private Category parent;
    
    private Integer parentId;
    
    @TableField(exist = false)
    private List<Category> children = new ArrayList<>();
    
    private Integer level = 1;
    
    private Integer sort = 0;
    
    private String description;
    
    private Integer status = 1;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private List<News> news = new ArrayList<>();
    
    // 添加子分类的便捷方法
    public void addChild(Category child) {
        children.add(child);
        child.setParent(this);
        child.setLevel(this.level + 1);
    }
} 