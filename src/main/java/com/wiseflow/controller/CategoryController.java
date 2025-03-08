package com.wiseflow.controller;

import com.wiseflow.config.CacheConfig;
import com.wiseflow.entity.Category;
import com.wiseflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;
    
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.findAll();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        Category category = categoryService.findById(id);
        return ResponseEntity.ok(category);
    }
    
    @GetMapping("/top")
    public ResponseEntity<List<Category>> getTopCategories() {
        List<Category> topCategories = categoryService.findTopCategories();
        return ResponseEntity.ok(topCategories);
    }
    
    @GetMapping("/children/{parentId}")
    public ResponseEntity<List<Category>> getChildCategories(@PathVariable Integer parentId) {
        List<Category> childCategories = categoryService.findChildCategories(parentId);
        return ResponseEntity.ok(childCategories);
    }
    
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Category>> getCategoriesByLevel(@PathVariable Integer level) {
        List<Category> categories = categoryService.findByLevel(level);
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/tree")
    public ResponseEntity<List<Category>> getCategoryTree() {
        List<Category> categoryTree = categoryService.findCategoryTree();
        return ResponseEntity.ok(categoryTree);
    }
    
    /**
     * 保存分类
     * 清除分类相关缓存
     */
    @PostMapping
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_NEWS_COUNT, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_LIST, allEntries = true)
    })
    public ResponseEntity<Category> saveCategory(@RequestBody Category category) {
        log.info("保存分类: {}", category.getName());
        return ResponseEntity.ok(categoryService.save(category));
    }
    
    /**
     * 更新分类
     * 清除分类相关缓存
     */
    @PutMapping("/{id}")
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_NEWS_COUNT, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_LIST, allEntries = true)
    })
    public ResponseEntity<Category> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        log.info("更新分类: id={}, name={}", id, category.getName());
        category.setId(id);
        return ResponseEntity.ok(categoryService.update(category));
    }
    
    /**
     * 删除分类
     * 清除分类相关缓存
     */
    @DeleteMapping("/{id}")
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_NEWS_COUNT, key = "#id"),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_LIST, allEntries = true)
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        log.info("删除分类: id={}", id);
        categoryService.delete(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 启用/禁用分类
     * 清除分类相关缓存
     */
    @PutMapping("/{id}/status")
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY_LIST, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_NEWS_LIST, allEntries = true)
    })
    public ResponseEntity<Void> toggleCategoryStatus(@PathVariable Integer id, @RequestParam boolean enabled) {
        log.info("切换分类状态: id={}, enabled={}", id, enabled);
        categoryService.toggleStatus(id, enabled);
        return ResponseEntity.ok().build();
    }
} 