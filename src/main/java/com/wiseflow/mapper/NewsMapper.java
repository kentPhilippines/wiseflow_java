package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 新闻数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {
    
    /**
     * 根据URL查询新闻
     */
    @Select("SELECT * FROM wf_news WHERE url = #{url} LIMIT 1")
    News selectByUrl(@Param("url") String url);
    
    /**
     * 检查URL是否存在
     */
    @Select("SELECT COUNT(*) FROM wf_news WHERE url = #{url}")
    int countByUrl(@Param("url") String url);
    
    /**
     * 根据分类ID查询新闻
     * @param domainConfig 
     */
    @Select("SELECT n.* FROM wf_news n WHERE n.category_id = #{categoryId} AND n.domain_config = #{domainConfig}")
    IPage<News> selectByCategoryId(Page<News> page, @Param("categoryId") Integer categoryId, @Param("domainConfig") String domainConfig);
    
    /**
     * 根据分类名称查询新闻
     * @param domainConfig 
     */
    @Select("SELECT n.*, " +
            "(SELECT i.url FROM wf_news_image i " +
            "WHERE i.news_id = n.id " +
            "ORDER BY i.is_cover DESC, i.position ASC " +
            "LIMIT 1) as cover_image " +
            "FROM wf_news n " +
            "JOIN wf_category c ON n.category_id = c.id " +
            "WHERE c.name = #{categoryName} AND n.domain_config = #{domainConfig} " +
            "ORDER BY n.publish_time DESC")
    IPage<News> selectByCategoryName(Page<News> page, @Param("categoryName") String categoryName, @Param("domainConfig") String domainConfig);
    
    /**
     * 查询热门新闻
     * @param domainConfig 
     */
    @Select("SELECT * FROM wf_news WHERE is_hot = 1 AND domain_config = #{domainConfig} ORDER BY crawl_time DESC")
    List<News> selectHotNews(Page<News> page, @Param("domainConfig") String domainConfig);
    
    /**
     * 查询推荐新闻
     * @param domainConfig 
     */
    @Select("SELECT * FROM wf_news WHERE is_recommend = 1 AND domain_config = #{domainConfig} ORDER BY crawl_time DESC")
    IPage<News> selectRecommendNews(Page<News> page, @Param("domainConfig") String domainConfig);
    
    /**
     * 查询置顶新闻
     * @param domainConfig 
     */
    @Select("SELECT * FROM wf_news WHERE is_top = 1 AND domain_config = #{domainConfig} ORDER BY crawl_time DESC")
    List<News> selectTopNews(@Param("domainConfig") String domainConfig);
    
    /**
     * 根据标题搜索新闻
     * @param domainConfig 
     */
    @Select("SELECT * FROM wf_news WHERE title LIKE CONCAT('%', #{keyword}, '%') AND domain_config = #{domainConfig} ORDER BY crawl_time DESC")
    IPage<News> searchByTitle(Page<News> page, @Param("keyword") String keyword, @Param("domainConfig") String domainConfig);
    
    /**
     * 综合搜索新闻
     * @param domainConfig 
     */
    @Select("SELECT n.* FROM wf_news n LEFT JOIN wf_news_content c ON n.id = c.news_id " +
            "WHERE n.title LIKE CONCAT('%', #{keyword}, '%') OR c.content LIKE CONCAT('%', #{keyword}, '%') AND n.domain_config = #{domainConfig} ORDER BY crawl_time DESC")
    IPage<News> search(Page<News> page, @Param("keyword") String keyword, @Param("domainConfig") String domainConfig);
    
    /**
     * 高级搜索
     * @param domainConfig 
     */
    @Select("<script>" +
            "SELECT n.* FROM wf_news n WHERE 1=1 " +
            "<if test='title != null and title != \"\"'>AND n.title LIKE CONCAT('%', #{title}, '%') </if>" +
            "<if test='categoryId != null'>AND n.category_id = #{categoryId} </if>" +
            "<if test='startDate != null'>AND n.crawl_time &gt;= #{startDate} </if>" +
            "<if test='endDate != null'>AND n.crawl_time &lt;= #{endDate} </if>" +
            "AND n.domain_config = #{domainConfig} " +
            "ORDER BY n.crawl_time DESC" +
            "</script>")
    IPage<News> advancedSearch(Page<News> page, 
                              @Param("title") String title,
                              @Param("categoryId") Integer categoryId,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate, @Param("domainConfig") String domainConfig);
    
    /**
     * 查询所有新闻，包含图片
     * @param domainConfig 
     */
    @Select("SELECT n.* FROM wf_news n WHERE n.domain_config = #{domainConfig} ORDER BY n.crawl_time DESC")
    Page<News> selectAllWithImages(Page<News> page, @Param("domainConfig") String domainConfig);
    
    /**
     * 查询按照时间和热度交叉排序的新闻
     * 使用加权算法，综合考虑发布时间的新鲜度和浏览量
     * @param domainConfig 
     */
    @Select("SELECT n.*, " +
            "(SELECT i.url FROM wf_news_image i " +
            "WHERE i.news_id = n.id " +
            "ORDER BY i.is_cover DESC, i.position ASC " +
            "LIMIT 1) as cover_image " +
            "FROM wf_news n " +
            "ORDER BY (CASE " +
            "   WHEN n.is_top = 1 THEN 3 " +
            "   WHEN n.is_hot = 1 THEN 2 " +
            "   WHEN n.is_recommend = 1 THEN 1 " +
            "   ELSE 0 END) DESC, " +
            "(n.view_count * 0.4 + (UNIX_TIMESTAMP(n.publish_time) / UNIX_TIMESTAMP(NOW())) * 0.6) DESC")
    IPage<News> selectHotAndRecentNews(Page<News> page, @Param("domainConfig") String domainConfig);
    
    /**
     * 根据ID查询新闻详情，包含内容、图片和标签等所有关联数据
     */
    @Select("SELECT n.* FROM wf_news n WHERE n.id = #{id}")
    News selectDetailById(@Param("id") Integer id);
    
    /**
     * 查询新闻列表，包含第一张图片
     * @param domainConfig 
     */
    @Select("SELECT n.*, " +
            "(SELECT i.url FROM wf_news_image i " +
            "WHERE i.news_id = n.id " +
            "ORDER BY i.is_cover DESC, i.position ASC " +
            "LIMIT 1) as cover_image " +
            "FROM wf_news n " +
            "ORDER BY n.publish_time DESC")
    Page<News> selectNewsWithFirstImage(Page<News> page, @Param("domainConfig") String domainConfig);

    @Select("SELECT * FROM wf_news WHERE domain_config IS NULL ORDER BY id ASC")
    List<News> selectUnassignedNews();
    
    @Update("UPDATE wf_news SET domain_config = #{domainConfig}, update_time = #{updateTime} WHERE id = #{id}")
    void updateDomain(News news);

    /**
     * 统计指定时间范围内某个域名的文章数量（按发布时间）
     */
    @Select("SELECT COUNT(*) FROM wf_news WHERE domain_config = #{domain} AND publish_time >= #{startTime} AND publish_time < #{endTime}")
    int countByDomainConfigAndPublishTimeBetween(
        @Param("domain") String domain,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计指定域名在指定日期和分类的文章数量
     */
    @Select("SELECT COUNT(*) FROM wf_news WHERE domain_config = #{domain} " +
            "AND DATE(publish_time) = #{date} " +
            "AND category_name = #{categoryName}")
    int countByDomainAndDateAndCategory(@Param("domain") String domain,
                                      @Param("date") LocalDate date,
                                      @Param("categoryName") String categoryName);

    /**
     * 搜索文章（包含内容）
     */
    @Select({
        "<script>",
        "SELECT n.*, nc.content as content ",
        "FROM wf_news n ",
        "LEFT JOIN wf_news_content nc ON n.id = nc.news_id ",
        "WHERE n.domain_config = #{domain} ",
        "<if test='title != null and title != \"\"'>",
        "    AND n.title LIKE CONCAT('%', #{title}, '%')",
        "</if>",
        "<if test='status != null'>",
        "    AND n.status = #{status}",
        "</if>",
        "<if test='startTime != null'>",
        "    AND n.publish_time >= #{startTime}",
        "</if>",
        "<if test='endTime != null'>",
        "    AND n.publish_time &lt; #{endTime}",
        "</if>",
        "ORDER BY n.publish_time DESC",
        "</script>"
    })
    IPage<News> searchWithContent(Page<News> page,
                                @Param("domain") String domain,
                                @Param("title") String title,
                                @Param("status") Integer status,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM wf_news WHERE is_comment = 0 AND domain_config = #{domain} AND publish_time >= #{time} ORDER BY id ASC LIMIT #{LIMIT}")
    List<News> searchIsComment(@Param("time") LocalDateTime time, @Param("LIMIT") Integer LIMIT, @Param("domain") String domain);

    @Select("SELECT " +
            "CASE " +
            "  WHEN domain_config IS NULL OR domain_config = '' THEN '未分配' " +
            "  ELSE '已分配' " +
            "END as type, " +
            "category_name as name, " +
            "COUNT(*) as value " +
            "FROM wf_news " +
            "GROUP BY " +
            "CASE " +
            "  WHEN domain_config IS NULL OR domain_config = '' THEN '未分配' " +
            "  ELSE '已分配' " +
            "END, " +
            "category_name")
    List<Map<String, Object>> selectArticleCounts();

    /**
     * 获取最早的文章时间
     */
    @Select("SELECT MIN(publish_time) FROM wf_news")
    LocalDateTime selectEarliestNewsTime();
    
    /**
     * 获取最新的文章时间
     */
    @Select("SELECT MAX(publish_time) FROM wf_news")
    LocalDateTime selectLatestNewsTime();
}