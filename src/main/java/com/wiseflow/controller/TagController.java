package com.wiseflow.controller;

import com.wiseflow.config.CacheConfig;
import com.wiseflow.entity.Tag;
import com.wiseflow.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 保存标签
     * 清除标签缓存
     */
    @PostMapping
    @CacheEvict(value = CacheConfig.CACHE_TAG_LIST, allEntries = true)
    public ResponseEntity<Tag> saveTag(@RequestBody Tag tag) {
        log.info("保存标签: {}", tag.getName());
        return ResponseEntity.ok(tagService.save(tag));
    }

    /**
     * 更新标签
     * 清除标签缓存
     */
    @PutMapping("/{id}")
    @CacheEvict(value = CacheConfig.CACHE_TAG_LIST, allEntries = true)
    public ResponseEntity<Tag> updateTag(@PathVariable Integer id, @RequestBody Tag tag) {
        log.info("更新标签: id={}, name={}", id, tag.getName());
        tag.setId(id);
        return ResponseEntity.ok(tagService.save(tag));
    }

    /**
     * 删除标签
     * 清除标签缓存
     */
    @DeleteMapping("/{id}")
    @CacheEvict(value = CacheConfig.CACHE_TAG_LIST, allEntries = true)
    public ResponseEntity<Void> deleteTag(@PathVariable Integer id) {
        log.info("删除标签: id={}", id);
        tagService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量删除标签
     * 清除标签缓存
     */
    @DeleteMapping("/batch")
    @CacheEvict(value = CacheConfig.CACHE_TAG_LIST, allEntries = true)
    public ResponseEntity<Void> batchDeleteTags(@RequestBody List<Integer> ids) {
        log.info("批量删除标签: ids={}", ids);
        // 逐个删除标签
        ids.forEach(tagService::deleteById);
        return ResponseEntity.ok().build();
    }

    // ... existing code ...
} 