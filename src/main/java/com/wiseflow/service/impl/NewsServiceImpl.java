package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wiseflow.entity.News;
import com.wiseflow.entity.NewsContent;
import com.wiseflow.entity.NewsImage;
import com.wiseflow.entity.Tag;
import com.wiseflow.mapper.NewsContentMapper;
import com.wiseflow.mapper.NewsImageMapper;
import com.wiseflow.mapper.NewsMapper;
import com.wiseflow.mapper.TagMapper;
import com.wiseflow.service.NewsService;
import com.wiseflow.service.SeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 新闻服务实现类
 * 使用MyBatis-Plus实现，不使用事务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {
    
    private final NewsMapper newsMapper;
    private final NewsContentMapper contentMapper;
    private final NewsImageMapper imageMapper;
    private final TagMapper tagMapper;
    private final SeoService seoService;
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public News save1(News news) {
        // 进行SEO优化
        news = seoService.processSeoOptimization(news);
        
        if (news.getId() == null) {
            newsMapper.insert(news);
        } else {
            newsMapper.updateById(news);
        }
        return news;
    }
    
    @Override
    public Optional<News> findById(Integer id) {
        return Optional.ofNullable(newsMapper.selectById(id));
    }
    
    @Override
    public Optional<News> findByUrl(String url) {
        return Optional.ofNullable(newsMapper.selectByUrl(url));
    }
    
    @Override
    public boolean isUrlExists(String url) {
        return newsMapper.countByUrl(url) > 0;
    }
    
    @Override
    public Page<News> findAll(Page<News> pageable,String domainConfig) {
        return newsMapper.selectNewsWithFirstImage(pageable,domainConfig);
    }
    
    @Override
    public IPage<News> findByCategoryId(Integer categoryId, Page<News> pageable,String domainConfig) {
        return newsMapper.selectByCategoryId(pageable, categoryId,domainConfig);
    }
    
    @Override
    public IPage<News> findByCategoryName(String categoryName, Page<News> pageable,String domainConfig) {
        return newsMapper.selectByCategoryName(pageable, categoryName,domainConfig);
    }
    
    @Override
    public List<News> findHotNews(Page<News> pageable,String domainConfig) {
        return newsMapper.selectHotNews(pageable,domainConfig);
    }
    
    @Override
    public IPage<News> findRecommendNews(Page<News> pageable,String domainConfig) {
        return newsMapper.selectRecommendNews(pageable,domainConfig);
    }
    
    @Override
    public List<News> findTopNews(String domainConfig) {
        return newsMapper.selectTopNews(domainConfig);
    }
    
    @Override
    public IPage<News> searchByTitle(String keyword, Page<News> pageable,String domainConfig) {
        return newsMapper.searchByTitle(pageable, keyword,domainConfig);
    }
    
    @Override
    public IPage<News> search(String keyword, Page<News> pageable,String domainConfig) {
        return newsMapper.search(pageable, keyword,domainConfig);
    }
    
    @Override
    public IPage<News> search(String title, Integer categoryId, 
                            LocalDateTime startDate, LocalDateTime endDate, 
                            Page<News> pageable,String domainConfig) {
        return newsMapper.advancedSearch(pageable, title, categoryId, startDate, endDate,domainConfig);
    }
    
    @Override
    public void incrementViewCount(Integer id) {
        News news = findById(id).orElseThrow(() -> new NoSuchElementException("News not found with id: " + id));
        news.setViewCount(news.getViewCount() + 1);
        newsMapper.updateById(news);
    }
    
    @Override
    public void incrementCommentCount(Integer id) {
        News news = findById(id).orElseThrow(() -> new NoSuchElementException("News not found with id: " + id));
        news.setCommentCount(news.getCommentCount() + 1);
        newsMapper.updateById(news);
    }
    
    @Override
    public void incrementLikeCount(Integer id) {
        News news = findById(id).orElseThrow(() -> new NoSuchElementException("News not found with id: " + id));
        news.setLikeCount(news.getLikeCount() + 1);
        newsMapper.updateById(news);
    }
    
    @Override
    public void toggleTop(Integer id, boolean isTop) {
        News news = findById(id).orElseThrow(() -> new NoSuchElementException("News not found with id: " + id));
        news.setIsTop(isTop);
        newsMapper.updateById(news);
    }
    
    @Override
    public void toggleHot(Integer id, boolean isHot) {
        News news = findById(id).orElseThrow(() -> new NoSuchElementException("News not found with id: " + id));
        news.setIsHot(isHot);
        newsMapper.updateById(news);
    }
    
    @Override
    public void toggleRecommend(Integer id, boolean isRecommend) {
        News news = findById(id).orElseThrow(() -> new NoSuchElementException("News not found with id: " + id));
        news.setIsRecommend(isRecommend);
        newsMapper.updateById(news);
    }
    
    @Override
    public void delete(News news) {
        newsMapper.deleteById(news.getId());
    }
    
    @Override
    public void deleteById(Integer id) {
        newsMapper.deleteById(id);
    }
    
    @Override
    public void deleteAll(List<Integer> ids) {
        for (Integer id : ids) {
            newsMapper.deleteById(id);
        }
    }
    
    @Override
    public List<News> findAllWithImagesAndTags() {
        // 使用MyBatis-Plus的关联查询
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(News::getCrawlTime);
        return newsMapper.selectList(wrapper);
    }
    
    @Override
    public Page<News> findAllWithImages(Page<News> pageable,String domainConfig) {
        return newsMapper.selectAllWithImages(pageable,domainConfig);
    }
    
    @Override
    public IPage<News> findHotAndRecentNews(Page<News> pageable,String domainConfig) {
        // 实现按照时间和热度交叉排序的新闻查询
        return newsMapper.selectHotAndRecentNews(pageable,domainConfig  );
    }
    
    @Override
    public Optional<News> findDetailById(Integer id) {
        // 获取包含所有关联数据的新闻详情
        News news = newsMapper.selectById(id);
        if (news != null) {
            // 1. 加载内容
            NewsContent content = contentMapper.selectByNewsId(id);
            news.setContent(content);
            
            // 2. 加载图片
            List<NewsImage> images = imageMapper.selectByNewsId(id);
            news.setImages(images);
            
            // 3. 加载标签
            List<Tag> tags = tagMapper.selectTagsByNewsId(id);
            news.setTags(new HashSet<>(tags));
        }
        return Optional.ofNullable(news);
    }

    @Override
    public void isHotNews(Integer id) {

        News news = newsMapper.selectById(id);
        if (news != null) {
            news.setIsHot(true);
            newsMapper.updateById(news);
        }
        
    }

    @Override
    public List<News> findUnassignedNews() {
        return newsMapper.selectUnassignedNews();
    }

    @Override
    public void updateDomain(News news) {
        newsMapper.updateDomain(news);
    }

    @Override
    public int countNewsByDomainAndDate(String domain, LocalDate date) {
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();
        
        return newsMapper.countByDomainConfigAndPublishTimeBetween(
            domain,
            startTime,
            endTime
        );
    }

    @Override
    public int countNewsByDomainAndDateAndCategory(String domain, LocalDate date, String categoryName) {
        return newsMapper.countByDomainAndDateAndCategory(domain, date, categoryName);
    }

    @Override
    public News update(News news) {
        // 进行SEO优化
        news = seoService.processSeoOptimization(news);
        
        newsMapper.updateById(news);
        return newsMapper.selectById(news.getId());
    }

    @Override
    public IPage<News> searchWithContent(String title,
                                       Integer categoryId,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime,
                                       Page<News> page,
                                       String domain,
                                       Integer status) {
        return newsMapper.searchWithContent(page, domain, title, status, startTime, endTime);
    }

    @Override
    public List<News> searchIsComment(LocalDateTime time, Integer LIMIT, String domain) {
        return newsMapper.searchIsComment(time, LIMIT, domain);
    }


    @Override
    public void isComment(Integer id, boolean b) {
        News news = newsMapper.selectById(id);
        if (news != null) {
            News news1 = new News();
            news1.setId(id);
            news1.setIsComment(b);
            newsMapper.updateById(news1);
        }
    }


    @Override
    public Long countUnassignedNews() {
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(News::getDomainConfig)
                   .or()
                   .eq(News::getDomainConfig, "");
        return newsMapper.selectCount(queryWrapper);
    }

    @Override
    public long count() {
        return newsMapper.selectCount(null);
    }

    @Override
    public List<Map<String, Object>> getArticleCounts() {
        String sql = """
            SELECT 
                CASE 
                    WHEN domain_config IS NULL OR domain_config = '' 
                    THEN '未分配' 
                    ELSE '已分配' 
                END as type,
                category_name as name,
                COUNT(*) as value 
            FROM wf_news 
            GROUP BY 
                CASE 
                    WHEN domain_config IS NULL OR domain_config = '' 
                    THEN '未分配' 
                    ELSE '已分配' 
                END,
                category_name
            """;
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> getArticleTypes() {
        String sql = "SELECT DISTINCT\n" +
                "    id,\n" +
                "    type_name as name\n" +
                "FROM wf_article_type\n" +
                "WHERE deleted = 0\n" +
                "ORDER BY sort_order";
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public LocalDateTime getEarliestNewsTime() {
        return newsMapper.selectEarliestNewsTime();
    }

    @Override
    public LocalDateTime getLatestNewsTime() {
        return newsMapper.selectLatestNewsTime();
    }

    @Override
    public int countAssignedNews(String domain, String typeId, LocalDateTime startTime, LocalDateTime endTime) {
        Long l = newsMapper.selectCount(
                new LambdaQueryWrapper<News>()
                        .eq(News::getDomainConfig, domain)
                        .eq(News::getCategoryName, typeId)
                        .between(News::getPublishTime, startTime, endTime)
        );
        return l.intValue();
    }

    @Override
    public List<News> getUnassignedNews(String typeId, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        return newsMapper.selectList(
            new LambdaQueryWrapper<News>()
                .eq(News::getCategoryName, typeId)
                .between(News::getPublishTime, startTime, endTime)
                .isNull(News::getDomainConfig)
                .orderByAsc(News::getPublishTime)
                .last("LIMIT " + limit)
        );
    }

    @Override
    public void updateNews(News news) {
        newsMapper.updateById(news);
    }

    @Override
    public Long countUnassignedNews(String domain, LocalDateTime dayStart, LocalDateTime dayEnd) {

        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                   .between(News::getPublishTime, dayStart, dayEnd)
                   .isNull(News::getDomainConfig);

        return  newsMapper.selectCount(queryWrapper);
    }

    @Override
    public List<News> getArticlesByTimeRange(String domain, LocalDateTime randomDate, LocalDateTime windowEnd, int i) {
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                   .between(News::getPublishTime, randomDate, windowEnd)
                   .eq(News::getDomainConfig, domain);

        return  newsMapper.selectList(queryWrapper);
    }

    @Override
    public List<News> getUncommentedArticles(String domain) {
        LambdaQueryWrapper<News> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                   .eq(News::getDomainConfig, domain)
                   .eq(News::getIsComment,false);

        return  newsMapper.selectList(queryWrapper);
    }
} 