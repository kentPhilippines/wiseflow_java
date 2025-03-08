package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 分类数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    
    /**
     * 根据名称查询分类
     */
    @Select("SELECT * FROM wf_category WHERE name = #{name} LIMIT 1")
    Category selectByName(@Param("name") String name);
    
    /**
     * 根据代码查询分类
     */
    @Select("SELECT * FROM wf_category WHERE code = #{code} LIMIT 1")
    Category selectByCode(@Param("code") String code);
    
    /**
     * 查询所有顶级分类
     */
    @Select("SELECT * FROM wf_category WHERE parent_id IS NULL ORDER BY sort")
    List<Category> selectTopCategories();
    
    /**
     * 查询指定父分类的子分类
     */
    @Select("SELECT * FROM wf_category WHERE parent_id = #{parentId} ORDER BY sort")
    List<Category> selectByParentId(@Param("parentId") Integer parentId);
    
    /**
     * 查询指定层级的分类
     */
    @Select("SELECT * FROM wf_category WHERE level = #{level} ORDER BY sort")
    List<Category> selectByLevel(@Param("level") Integer level);
    
    /**
     * 查询分类树（仅查询顶级分类，子分类通过关联查询获取）
     */
    @Select("SELECT * FROM wf_category WHERE parent_id IS NULL ORDER BY sort")
    List<Category> selectCategoryTree();
    
    /**
     * 查询每个分类下的新闻数量
     * 返回包含分类ID和对应新闻数量的映射
     */
    @Select("SELECT c.id as categoryId, COUNT(n.id) as newsCount " +
            "FROM wf_category c " +
            "LEFT JOIN wf_news n ON c.id = n.category_id " +
            "GROUP BY c.id")
    List<Map<String, Object>> selectCategoryNewsCount();
    
    /**
     * 查询指定分类下的新闻数量
     */
    @Select("SELECT COUNT(*) FROM wf_news WHERE category_id = #{categoryId}")
    Integer countNewsByCategoryId(@Param("categoryId") Integer categoryId);
} 