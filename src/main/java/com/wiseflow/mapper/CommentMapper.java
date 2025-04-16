package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论数据访问接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    
    /**
     * 根据文章ID查询评论
     */
    @Select("SELECT * FROM wf_comment WHERE news_id = #{newsId} ORDER BY comment_time DESC")
    List<Comment> findByNewsId(@Param("newsId") Integer newsId);
    
    /**
     * 分页查询文章评论
     */
    @Select("SELECT * FROM wf_comment WHERE news_id = #{newsId} ORDER BY comment_time DESC")
    IPage<Comment> findByNewsIdPaged(Page<Comment> page, @Param("newsId") Integer newsId);
    
    /**
     * 查询域名下的所有评论
     */
    @Select("SELECT * FROM wf_comment WHERE domain_config = #{domainConfig} ORDER BY comment_time DESC")
    IPage<Comment> findByDomainConfigPaged(Page<Comment> page, @Param("domainConfig") String domainConfig);
    
    /**
     * 查询使用了指定关键词的评论
     */
    @Select("SELECT * FROM wf_comment WHERE seo_keyword_id = #{keywordId} ORDER BY comment_time DESC")
    List<Comment> findByKeywordId(@Param("keywordId") Integer keywordId);
    
    /**
     * 批量删除评论
     */
    @Update("<script>" +
            "DELETE FROM wf_comment WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchDelete(@Param("ids") List<Integer> ids);
    
    /**
     * 查询AI生成的评论
     */
    @Select("SELECT * FROM wf_comment WHERE is_ai_generated = 1 AND domain_config = #{domainConfig} ORDER BY comment_time DESC")
    IPage<Comment> findAiGeneratedPaged(Page<Comment> page, @Param("domainConfig") String domainConfig);
    
    /**
     * 统计文章评论数量
     */
    @Select("SELECT COUNT(*) FROM wf_comment WHERE news_id = #{newsId}")
    int countByNewsId(@Param("newsId") Integer newsId);
    
    /**
     * 点赞评论
     */
    @Update("UPDATE wf_comment SET like_count = like_count + 1 WHERE id = #{id}")
    void incrementLikeCount(@Param("id") Integer id);
} 