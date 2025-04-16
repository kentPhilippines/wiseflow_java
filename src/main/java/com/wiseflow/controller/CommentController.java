package com.wiseflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.Comment;
import com.wiseflow.entity.CommentRule;
import com.wiseflow.service.CommentRuleService;
import com.wiseflow.service.CommentService;
import com.wiseflow.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentRuleService commentRuleService;

    /**
     * 获取评论列表（分页）
     */
    @GetMapping("/list")
    public Result<IPage<Comment>> list(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "domainConfig") String domainConfig) {
        
        Page<Comment> page = new Page<>(current, size);
        IPage<Comment> result = commentService.findByDomainConfigPaged(domainConfig, page);
        return Result.success(result);
    }

    /**
     * 获取文章的评论列表（分页）
     */
    @GetMapping("/listByNewsId")
    public Result<IPage<Comment>> listByNewsId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "newsId") Integer newsId) {
        
        Page<Comment> page = new Page<>(current, size);
        IPage<Comment> result = commentService.findByNewsIdPaged(newsId, page);
        return Result.success(result);
    }

    /**
     * 获取AI生成的评论列表（分页）
     */
    @GetMapping("/listAiGenerated")
    public Result<IPage<Comment>> listAiGenerated(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "domainConfig") String domainConfig) {
        
        Page<Comment> page = new Page<>(current, size);
        IPage<Comment> result = commentService.findAiGeneratedPaged(domainConfig, page);
        return Result.success(result);
    }

    /**
     * 根据ID获取评论
     */
    @GetMapping("/{id}")
    public Result<Comment> getById(@PathVariable Integer id) {
        Comment comment = commentService.findById(id);
        return Result.success(comment);
    }

    /**
     * 保存评论
     */
    @PostMapping("/save")
    public Result<Comment> save(@RequestBody Comment comment) {
        Comment saved = commentService.save1(comment);
        return Result.success(saved);
    }

    /**
     * 更新评论
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Comment comment) {
        commentService.update(comment);
        return Result.success();
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        commentService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除评论
     */
    @PostMapping("/batchDelete")
    public Result<Void> batchDelete(@RequestBody List<Integer> ids) {
        commentService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 给评论点赞
     */
    @PostMapping("/like/{id}")
    public Result<Void> likeComment(@PathVariable Integer id) {
        commentService.likeComment(id);
        return Result.success();
    }

    /**
     * 生成AI评论
     */
    @PostMapping("/generateAiComments")
    public Result<Void> generateAiComments(
            @RequestParam Integer newsId,
            @RequestParam(defaultValue = "3") Integer count) {
        
        commentService.generateAiComments(newsId, count);
        return Result.success();
    }

} 