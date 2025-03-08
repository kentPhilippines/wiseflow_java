package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.NewsContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 新闻内容数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface NewsContentMapper extends BaseMapper<NewsContent> {
    
    /**
     * 根据新闻ID查询内容
     */
    @Select("SELECT * FROM wf_news_content WHERE news_id = #{newsId} LIMIT 1")
    NewsContent selectByNewsId(@Param("newsId") Integer newsId);
    
    /**
     * 根据关键词搜索内容
     */
    @Select("SELECT nc.* FROM wf_news_content nc WHERE nc.content LIKE CONCAT('%', #{keyword}, '%')")
    NewsContent selectByKeyword(@Param("keyword") String keyword);
} 