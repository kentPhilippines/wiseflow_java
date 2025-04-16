package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.SeoKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * SEO关键词Mapper接口
 */
@Mapper
public interface SeoKeywordMapper extends BaseMapper<SeoKeyword> {
    
    /**
     * 批量删除关键词
     */
    @Update("<script>" +
            "DELETE FROM wf_seo_keyword WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchDelete(@Param("ids") List<Long> ids);
    
    /**
     * 批量更新状态
     */
    @Update("<script>" +
            "UPDATE wf_seo_keyword SET enabled = #{enabled} WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchUpdateStatus(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);
    
    /**
     * 获取域名配置下的所有关键词
     */
    @Select("SELECT * FROM wf_seo_keyword WHERE domain_config = #{domainConfigId}")
    List<SeoKeyword> findByDomainConfig(@Param("domainConfigId") Long domainConfigId);
    
    /**
     * 获取首页展示的关键词
     */
    @Select("SELECT * FROM wf_seo_keyword WHERE domain_config = #{domainConfigId} AND show_on_homepage = 1 AND enabled = 1")
    List<SeoKeyword> findShowOnHomepage(@Param("domainConfigId") Long domainConfigId);
    
    /**
     * 根据类型获取关键词
     */
    @Select("SELECT * FROM wf_seo_keyword WHERE domain_config = #{domainConfigId} AND type = #{type} AND enabled = 1")
    List<SeoKeyword> findByType(@Param("domainConfigId") Long domainConfigId, @Param("type") Integer type);
    
    /**
     * 根据使用场景获取关键词
     */
    @Select("SELECT * FROM wf_seo_keyword WHERE domain_config = #{domainConfigId} AND use_scene = #{useScene} AND enabled = 1")
    List<SeoKeyword> findByUseScene(@Param("domainConfigId") Long domainConfigId, @Param("useScene") Integer useScene);
} 