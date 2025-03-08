package com.wiseflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.Tag;

import java.util.List;
import java.util.Optional;

/**
 * 标签服务接口
 * 使用MyBatis-Plus实现，不使用事务管理
 */
public interface TagService {

    /**
     * 保存标签
     */
    Tag save(Tag tag);
    
    /**
     * 根据ID查找标签
     */
    Optional<Tag> findById(Integer id);
    
    /**
     * 根据名称查找标签
     */
    Optional<Tag> findByName(String name);
    
    /**
     * 查找所有标签
     */
    List<Tag> findAll();
    
    /**
     * 分页查找标签
     */
    Page<Tag> findAll(Page<Tag> pageable);
    
    /**
     * 删除标签
     */
    void delete(Tag tag);
    
    /**
     * 根据ID删除标签
     */
    void deleteById(Integer id);
    
    /**
     * 查找热门标签
     */
    List<Tag> findHotTags(int limit);
} 