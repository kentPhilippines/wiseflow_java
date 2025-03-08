package com.wiseflow.service;

import com.wiseflow.entity.Category;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分类服务接口
 * 使用MyBatis-Plus实现，不使用事务管理
 */
public interface CategoryService {
    
    /**
     * 保存分类
     */
    Category save(Category category);
    
    /**
     * 根据ID查询分类
     */
    Category findById(Integer id);
    
    /**
     * 根据名称查询分类
     */
    Optional<Category> findByName(String name);
    
    /**
     * 根据代码查询分类
     */
    Optional<Category> findByCode(String code);
    
    /**
     * 查询所有分类
     */
    List<Category> findAll();
    
    /**
     * 查询所有顶级分类
     */
    List<Category> findTopCategories();
    
    /**
     * 查询指定父分类的子分类
     */
    List<Category> findChildCategories(Integer parentId);
    
    /**
     * 查询指定层级的分类
     */
    List<Category> findByLevel(Integer level);
    
    /**
     * 查询分类树
     */
    List<Category> findCategoryTree();
    
    /**
     * 更新分类
     */
    Category update(Category category);
    
    /**
     * 删除分类
     */
    void delete(Integer id);
    
    /**
     * 启用/禁用分类
     */
    void toggleStatus(Integer id, boolean enabled);
    
    /**
     * 获取所有分类及其新闻数量
     * 返回包含分类信息和新闻数量的列表
     */
    List<Map<String, Object>> findAllWithNewsCount();
    
    /**
     * 获取指定分类下的新闻数量
     */
    Integer getNewsCountByCategoryId(Integer categoryId);
} 