package com.wiseflow.service;

import com.wiseflow.entity.Category;
import com.wiseflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Transactional
    public Category save(Category category) {
        if (category.getId() == null && categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("分类名称已存在");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        if (!category.getArticles().isEmpty()) {
            throw new RuntimeException("Cannot delete category with articles");
        }
        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public Category findByName(String name) {
        return categoryRepository.findByName(name);
    }
} 