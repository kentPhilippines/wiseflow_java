package com.wiseflow.controller;

import com.wiseflow.entity.Category;
import com.wiseflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("category", new Category());
        return "categories/list";
    }

    @PostMapping
    public String create( Category category, BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "categories/list";
        }
        
        try {
            categoryService.save(category);
            redirectAttributes.addFlashAttribute("success", "分类创建成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.findById(id));
        return "categories/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, Category category,
                        BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "categories/edit";
        }
        
        try {
            category.setId(id);
            categoryService.save(category);
            redirectAttributes.addFlashAttribute("success", "分类更新成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("success", "分类删除成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categories";
    }
} 