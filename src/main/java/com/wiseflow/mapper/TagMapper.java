package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    
    /**
     * 根据名称查询标签
     */
    @Select("SELECT * FROM wf_tag WHERE name = #{name} LIMIT 1")
    Tag selectByName(@Param("name") String name);
    
    /**
     * 查询热门标签
     */
    @Select("SELECT * FROM wf_tag ORDER BY frequency DESC LIMIT #{limit}")
    List<Tag> selectHotTags(@Param("limit") int limit);
    
    /**
     * 根据新闻ID查询标签
     */
    @Select("SELECT t.* FROM wf_tag t JOIN wf_news_tag nt ON t.id = nt.tag_id WHERE nt.news_id = #{newsId}")
    List<Tag> selectTagsByNewsId(@Param("newsId") Integer newsId);
} 