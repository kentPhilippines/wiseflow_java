package com.wiseflow.controller;

import com.wiseflow.entity.SeoKeyword;
import com.wiseflow.service.SeoKeywordService;
import com.wiseflow.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * SEO关键词API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/seo")
public class SeoKeywordController {

    @Autowired
    private SeoKeywordService seoKeywordService;

    /**
     * 获取所有关键词
     */
    @GetMapping("/keywords")
    public List<SeoKeyword> getKeywords(@RequestParam String domain) {
        return seoKeywordService.getKeywordsByDomain(domain);
    }

    /**
     * 获取所有关键词
     */
    @GetMapping("/keywords/list")
    public Result<List<SeoKeyword>> list() {
        log.info("获取所有关键词");
        List<SeoKeyword> keywords = seoKeywordService.findAll();
        return Result.success(keywords);
    }

    /**
     * 获取启用的关键词
     */
    @GetMapping("/keywords/enabled")
    public Result<List<SeoKeyword>> listEnabled() {
        log.info("获取启用的关键词");
        List<SeoKeyword> keywords = seoKeywordService.findEnabled();
        return Result.success(keywords);
    }

    /**
     * 根据类型获取关键词
     */
    @GetMapping("/keywords/type/{type}")
    public Result<List<SeoKeyword>> listByType(@PathVariable Integer type) {
        log.info("根据类型获取关键词: type={}", type);
        if (type != 1 && type != 2) {
            return Result.error("无效的关键词类型");
        }
        List<SeoKeyword> keywords = seoKeywordService.findByType(type);
        return Result.success(keywords);
    }

    /**
     * 根据使用场景获取关键词
     */
    @GetMapping("/keywords/scene/{useScene}")
    public Result<List<SeoKeyword>> listByUseScene(@PathVariable Integer useScene) {
        log.info("根据使用场景获取关键词: useScene={}", useScene);
        if (useScene < 1 || useScene > 3) {
            return Result.error("无效的使用场景");
        }
        List<SeoKeyword> keywords = seoKeywordService.findByUseScene(useScene);
        return Result.success(keywords);
    }

    /**
     * 根据状态获取关键词
     */
    @GetMapping("/keywords/status/{enabled}")
    public Result<List<SeoKeyword>> listByStatus(@PathVariable Boolean enabled) {
        log.info("根据状态获取关键词: enabled={}", enabled);
        List<SeoKeyword> keywords = seoKeywordService.findByStatus(enabled);
        return Result.success(keywords);
    }

    /**
     * 保存关键词
     */
    @PostMapping("/keywords")
    public Result<SeoKeyword> save(@RequestBody SeoKeyword keyword) {
        log.info("保存关键词");
        
        // 验证必填参数
        if (keyword.getType() == null  ) {
            return Result.error("无效的关键词类型");
        }
        if (keyword.getUseScene() == null  ) {
            return Result.error("无效的使用场景");
        }
        if (keyword.getEnabled() == null) {
            keyword.setEnabled(true); // 设置默认值
        }
        
        try {
            SeoKeyword saved = seoKeywordService.save1(keyword);
            return Result.success(saved);
        } catch (Exception e) {
            log.error("保存关键词失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 更新关键词
     */
    @PutMapping("/keywords/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SeoKeyword keyword) {
        log.info("更新关键词: id={}", id);
        
        // 验证必填参数
        if (keyword.getType() == null) {
            return Result.error("无效的关键词类型");
        }
        if (keyword.getUseScene() == null ) {
            return Result.error("无效的使用场景");
        }
        if (keyword.getEnabled() == null) {
            keyword.setEnabled(true); // 设置默认值
        }
        
        keyword.setId(id.intValue());
        seoKeywordService.update(keyword);
        return Result.success();
    }

    /**
     * 删除关键词
     */
    @DeleteMapping("/keywords/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除关键词: id={}", id);
        seoKeywordService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除关键词
     */
    @DeleteMapping("/keywords/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        log.info("批量删除关键词: ids={}", ids);
        seoKeywordService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 批量更新状态
     */
    @PutMapping("/keywords/batch/status")
    public Result<Void> batchUpdateStatus(
            @RequestParam("ids[]") List<Long> ids, 
            @RequestParam("enabled") Boolean enabled) {
        log.info("批量更新关键词状态: ids={}, enabled={}", ids, enabled);
        seoKeywordService.batchUpdateStatus(ids, enabled);
        return Result.success();
    }

    /**
     * 获取单个关键词
     */
    @GetMapping("/keywords/{id}")
    public Result<SeoKeyword> get(@PathVariable Long id) {
        log.info("获取单个关键词: id={}", id);
        SeoKeyword keyword = seoKeywordService.findById(id);
        return Result.success(keyword);
    }
    
    /**
     * 导出关键词
     */
    @GetMapping("/keywords/export")
    public ResponseEntity<byte[]> exportKeywords() {
        log.info("导出关键词");
        
        List<SeoKeyword> keywords = seoKeywordService.findAll();
        
        // 构建CSV内容
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("类型,使用场景,最大插入次数,允许标题,评论情感,最大重复次数,状态,备注\n");
        
        for (SeoKeyword keyword : keywords) {
            csvContent.append(keyword.getType() == 1 ? "主关键词" : "长尾关键词").append(",");
            
            // 使用场景
            String useScene = "";
            switch (keyword.getUseScene()) {
                case 1: useScene = "文章内容"; break;
                case 2: useScene = "评论内容"; break;
                case 3: useScene = "两者都用"; break;
                default: useScene = "-";
            }
            csvContent.append(useScene).append(",");
            
            csvContent.append(keyword.getMaxInsertions() != null ? keyword.getMaxInsertions() : "-").append(",");
            csvContent.append(keyword.getAllowTitle() != null && keyword.getAllowTitle() ? "是" : "否").append(",");
            
            // 评论情感
            String sentiment = "";
            if (keyword.getCommentSentiment() != null) {
                switch (keyword.getCommentSentiment()) {
                    case 1: sentiment = "积极"; break;
                    case 2: sentiment = "中性"; break;
                    case 3: sentiment = "消极"; break;
                    default: sentiment = "-";
                }
            } else {
                sentiment = "-";
            }
            csvContent.append(sentiment).append(",");
            
            csvContent.append(keyword.getMaxCommentRepeat() != null ? keyword.getMaxCommentRepeat() : "-").append(",");
            csvContent.append(keyword.getEnabled() != null && keyword.getEnabled() ? "启用" : "禁用").append(",");
            csvContent.append(keyword.getRemark() != null ? keyword.getRemark().replace(",", " ") : "").append("\n");
        }
        
        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        String filename = "keywords.csv";
        headers.setContentDispositionFormData("attachment", filename);
        
        // 返回CSV文件
        return new ResponseEntity<>(csvContent.toString().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }
} 