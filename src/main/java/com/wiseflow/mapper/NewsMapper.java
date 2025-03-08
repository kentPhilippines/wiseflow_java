package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

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
     */
    @Select("SELECT n.* FROM wf_news n WHERE n.category_id = #{categoryId}")
    IPage<News> selectByCategoryId(Page<News> page, @Param("categoryId") Integer categoryId);
    
    /**
     * 根据分类名称查询新闻
     */
    @Select("SELECT n.*, " +
            "(SELECT i.url FROM wf_news_image i " +
            "WHERE i.news_id = n.id " +
            "ORDER BY i.is_cover DESC, i.position ASC " +
            "LIMIT 1) as cover_image " +
            "FROM wf_news n " +
            "JOIN wf_category c ON n.category_id = c.id " +
            "WHERE c.name = #{categoryName} " +
            "ORDER BY n.publish_time DESC")
    IPage<News> selectByCategoryName(Page<News> page, @Param("categoryName") String categoryName);
    
    /**
     * 查询热门新闻
     */
    @Select("SELECT * FROM wf_news WHERE is_hot = 1 ORDER BY crawl_time DESC")
    List<News> selectHotNews(Page<News> page);
    
    /**
     * 查询推荐新闻
     */
    @Select("SELECT * FROM wf_news WHERE is_recommend = 1 ORDER BY crawl_time DESC")
    IPage<News> selectRecommendNews(Page<News> page);
    
    /**
     * 查询置顶新闻
     */
    @Select("SELECT * FROM wf_news WHERE is_top = 1 ORDER BY crawl_time DESC")
    List<News> selectTopNews();
    
    /**
     * 根据标题搜索新闻
     */
    @Select("SELECT * FROM wf_news WHERE title LIKE CONCAT('%', #{keyword}, '%')")
    IPage<News> searchByTitle(Page<News> page, @Param("keyword") String keyword);
    
    /**
     * 综合搜索新闻
     */
    @Select("SELECT n.* FROM wf_news n LEFT JOIN wf_content c ON n.id = c.news_id " +
            "WHERE n.title LIKE CONCAT('%', #{keyword}, '%') OR c.content LIKE CONCAT('%', #{keyword}, '%')")
    IPage<News> search(Page<News> page, @Param("keyword") String keyword);
    
    /**
     * 高级搜索
     */
    @Select("<script>" +
            "SELECT n.* FROM wf_news n WHERE 1=1 " +
            "<if test='title != null and title != \"\"'>AND n.title LIKE CONCAT('%', #{title}, '%') </if>" +
            "<if test='categoryId != null'>AND n.category_id = #{categoryId} </if>" +
            "<if test='startDate != null'>AND n.crawl_time &gt;= #{startDate} </if>" +
            "<if test='endDate != null'>AND n.crawl_time &lt;= #{endDate} </if>" +
            "ORDER BY n.crawl_time DESC" +
            "</script>")
    IPage<News> advancedSearch(Page<News> page, 
                              @Param("title") String title,
                              @Param("categoryId") Integer categoryId,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * 查询所有新闻，包含图片
     */
    @Select("SELECT n.* FROM wf_news n ORDER BY n.crawl_time DESC")
    Page<News> selectAllWithImages(Page<News> page);
    
    /**
     * 查询按照时间和热度交叉排序的新闻
     * 使用加权算法，综合考虑发布时间的新鲜度和浏览量
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
    IPage<News> selectHotAndRecentNews(Page<News> page);
    
    /**
     * 根据ID查询新闻详情，包含内容、图片和标签等所有关联数据
     */
    @Select("SELECT n.* FROM wf_news n WHERE n.id = #{id}")
    News selectDetailById(@Param("id") Integer id);
    
    /**
     * 查询新闻列表，包含第一张图片
     */
    @Select("SELECT n.*, " +
            "(SELECT i.url FROM wf_news_image i " +
            "WHERE i.news_id = n.id " +
            "ORDER BY i.is_cover DESC, i.position ASC " +
            "LIMIT 1) as cover_image " +
            "FROM wf_news n " +
            "ORDER BY n.publish_time DESC")
    Page<News> selectNewsWithFirstImage(Page<News> page);
} 