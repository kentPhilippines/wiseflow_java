package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.NewsImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 新闻图片数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface NewsImageMapper extends BaseMapper<NewsImage> {
    
    /**
     * 根据新闻ID查询图片
     */
    @Select("SELECT * FROM wf_news_image WHERE news_id = #{newsId} ORDER BY position ASC")
    List<NewsImage> selectByNewsId(@Param("newsId") Integer newsId);
    
    /**
     * 查询封面图片
     */
    @Select("SELECT * FROM wf_news_image WHERE news_id = #{newsId} AND is_cover = 1 LIMIT 1")
    NewsImage selectCoverByNewsId(@Param("newsId") Integer newsId);
} 