package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wiseflow.entity.Category;
import com.wiseflow.mapper.CategoryMapper;
import com.wiseflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 分类服务实现类
 * 使用MyBatis-Plus实现，不使用事务管理
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryMapper categoryMapper;
    
    @Override
    public Category save(Category category) {
        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return category;
    }
    
    @Override
    public Category findById(Integer id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new NoSuchElementException("Category not found with id: " + id);
        }
        return category;
    }
    
    @Override
    public Optional<Category> findByName(String name) {
        return Optional.ofNullable(categoryMapper.selectByName(name));
    }
    
    @Override
    public Optional<Category> findByCode(String code) {
        return Optional.ofNullable(categoryMapper.selectByCode(code));
    }
    
    @Override
    public List<Category> findAll() {
        return categoryMapper.selectList(null);
    }
    
    @Override
    public List<Category> findTopCategories() {
        return categoryMapper.selectTopCategories();
    }
    
    @Override
    public List<Category> findChildCategories(Integer parentId) {
        return categoryMapper.selectByParentId(parentId);
    }
    
    @Override
    public List<Category> findByLevel(Integer level) {
        return categoryMapper.selectByLevel(level);
    }
    
    @Override
    public List<Category> findCategoryTree() {
        return categoryMapper.selectCategoryTree();
    }
    
    @Override
    public Category update(Category category) {
        // 确保分类存在
        findById(category.getId());
        categoryMapper.updateById(category);
        return category;
    }
    
    @Override
    public void delete(Integer id) {
        Category category = findById(id);
        
        // 检查是否有子分类
        List<Category> children = findChildCategories(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete category with children");
        }
        
        // 检查是否有关联的新闻
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getId, id);
        Category fullCategory = categoryMapper.selectOne(wrapper);
        if (fullCategory != null && fullCategory.getNews() != null && !fullCategory.getNews().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated news");
        }
        
        categoryMapper.deleteById(id);
    }
    
    @Override
    public void toggleStatus(Integer id, boolean enabled) {
        Category category = findById(id);
        category.setStatus(enabled ? 1 : 0);
        categoryMapper.updateById(category);
    }
    
    @Override
    public List<Map<String, Object>> findAllWithNewsCount() {
        // 获取所有分类
        List<Category> categories = findAll();
        
        // 获取所有分类的新闻数量
        List<Map<String, Object>> categoryCounts = categoryMapper.selectCategoryNewsCount();
        
        // 将新闻数量映射到分类ID
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Map<String, Object> count : categoryCounts) {
            Integer categoryId = ((Number) count.get("categoryId")).intValue();
            Integer newsCount = ((Number) count.get("newsCount")).intValue();
            countMap.put(categoryId, newsCount);
        }
        
        // 构建结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> item = new HashMap<>();
            item.put("category", category);
            item.put("newsCount", countMap.getOrDefault(category.getId(), 0));
            result.add(item);
        }
        
        return result;
    }
    
    @Override
    public Integer getNewsCountByCategoryId(Integer categoryId) {
        return categoryMapper.countNewsByCategoryId(categoryId);
    }
} 