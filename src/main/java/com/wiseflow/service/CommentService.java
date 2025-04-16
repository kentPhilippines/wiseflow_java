package com.wiseflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.Comment;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {
    
    /**
     * 保存评论
     */
    Comment save1(Comment comment);
    
    /**
     * 更新评论
     */
    void update(Comment comment);
    
    /**
     * 删除评论
     */
    void delete(Integer id);
    
    /**
     * 批量删除评论
     */
    void batchDelete(List<Integer> ids);
    
    /**
     * 根据ID查询评论
     */
    Comment findById(Integer id);
    
    /**
     * 根据文章ID查询评论列表
     */
    List<Comment> findByNewsId(Integer newsId);
    
    /**
     * 分页查询文章评论
     */
    IPage<Comment> findByNewsIdPaged(Integer newsId, Page<Comment> page);
    
    /**
     * 分页查询域名下的所有评论
     */
    IPage<Comment> findByDomainConfigPaged(String domainConfig, Page<Comment> page);
    
    /**
     * 查询使用了指定关键词的评论
     */
    List<Comment> findByKeywordId(Integer keywordId);
    
    /**
     * 分页查询AI生成的评论
     */
    IPage<Comment> findAiGeneratedPaged(String domainConfig, Page<Comment> page);
    
    /**
     * 统计文章评论数量
     */
    int countByNewsId(Integer newsId);
    
    /**
     * 点赞评论
     */
    void likeComment(Integer id);
    
    /**
     * 生成AI评论
     * @param newsId 文章ID
     * @param count 生成数量
     */
    void generateAiComments(Integer newsId, int count);
} 