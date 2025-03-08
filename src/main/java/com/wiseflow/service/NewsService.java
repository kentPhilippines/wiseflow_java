package com.wiseflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.News;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 新闻服务接口
 * 使用MyBatis-Plus实现，不使用事务管理
 */
public interface NewsService {
    
    /**
     * 保存新闻
     */
    News save(News news);
    
    /**
     * 根据ID查询新闻
     */
    Optional<News> findById(Integer id);
    
    /**
     * 根据URL查询新闻
     */
    Optional<News> findByUrl(String url);
    
    /**
     * 检查URL是否存在
     */
    boolean isUrlExists(String url);
    
    /**
     * 查询所有新闻
     */
    IPage<News> findAll(Page<News> pageable);
    
    /**
     * 根据分类ID查询新闻
     */
    IPage<News> findByCategoryId(Integer categoryId, Page<News> pageable);
    
    /**
     * 根据分类名称查询新闻
     */
    IPage<News> findByCategoryName(String categoryName, Page<News> pageable);
    
    /**
     * 查询热门新闻
     */
    List<News> findHotNews(Page<News> pageable);
    
    /**
     * 查询推荐新闻
     */
    IPage<News> findRecommendNews(Page<News> pageable);
    
    /**
     * 查询置顶新闻
     */
    List<News> findTopNews();
    
    /**
     * 根据标题搜索新闻
     */
    IPage<News> searchByTitle(String keyword, Page<News> pageable);
    
    /**
     * 综合搜索新闻
     */
    IPage<News> search(String keyword, Page<News> pageable);
    
    /**
     * 高级搜索
     */
    IPage<News> search(String title, Integer categoryId, 
                      LocalDateTime startDate, LocalDateTime endDate, 
                      Page<News> pageable);
    
    /**
     * 增加浏览量
     */
    void incrementViewCount(Integer id);
    
    /**
     * 增加评论数
     */
    void incrementCommentCount(Integer id);
    
    /**
     * 增加点赞数
     */
    void incrementLikeCount(Integer id);
    
    /**
     * 切换置顶状态
     */
    void toggleTop(Integer id, boolean isTop);
    
    /**
     * 切换热门状态
     */
    void toggleHot(Integer id, boolean isHot);
    
    /**
     * 切换推荐状态
     */
    void toggleRecommend(Integer id, boolean isRecommend);
    
    /**
     * 删除新闻
     */
    void delete(News news);
    
    /**
     * 根据ID删除新闻
     */
    void deleteById(Integer id);
    
    /**
     * 批量删除新闻
     */
    void deleteAll(List<Integer> ids);
    
    /**
     * 查询所有新闻，包含图片和标签
     */
    List<News> findAllWithImagesAndTags();
    
    /**
     * 查询所有新闻，包含图片
     */
    Page<News> findAllWithImages(Page<News> pageable);
    
    /**
     * 查询按照时间和热度交叉排序的新闻
     * 默认情况下的新闻列表，综合考虑时间新鲜度和热度
     */
    IPage<News> findHotAndRecentNews(Page<News> pageable);
    
    /**
     * 根据ID查询新闻详情，包含内容、图片和标签等所有关联数据
     */
    Optional<News> findDetailById(Integer id);

    void isHotNews(Integer id);
} 