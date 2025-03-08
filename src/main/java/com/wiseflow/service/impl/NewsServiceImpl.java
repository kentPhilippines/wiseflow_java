package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.News;
import com.wiseflow.entity.NewsContent;
import com.wiseflow.entity.NewsImage;
import com.wiseflow.entity.Tag;
import com.wiseflow.mapper.NewsContentMapper;
import com.wiseflow.mapper.NewsImageMapper;
import com.wiseflow.mapper.NewsMapper;
import com.wiseflow.mapper.TagMapper;
import com.wiseflow.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 新闻服务实现类
 * 使用MyBatis-Plus实现，不使用事务管理
 */
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {
    
    private final NewsMapper newsMapper;
    private final NewsContentMapper contentMapper;
    private final NewsImageMapper imageMapper;
    private final TagMapper tagMapper;
    
    @Override
    public News save(News news) {
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
    public Page<News> findAll(Page<News> pageable) {
        return newsMapper.selectNewsWithFirstImage(pageable);
    }
    
    @Override
    public IPage<News> findByCategoryId(Integer categoryId, Page<News> pageable) {
        return newsMapper.selectByCategoryId(pageable, categoryId);
    }
    
    @Override
    public IPage<News> findByCategoryName(String categoryName, Page<News> pageable) {
        return newsMapper.selectByCategoryName(pageable, categoryName);
    }
    
    @Override
    public List<News> findHotNews(Page<News> pageable) {
        return newsMapper.selectHotNews(pageable);
    }
    
    @Override
    public IPage<News> findRecommendNews(Page<News> pageable) {
        return newsMapper.selectRecommendNews(pageable);
    }
    
    @Override
    public List<News> findTopNews() {
        return newsMapper.selectTopNews();
    }
    
    @Override
    public IPage<News> searchByTitle(String keyword, Page<News> pageable) {
        return newsMapper.searchByTitle(pageable, keyword);
    }
    
    @Override
    public IPage<News> search(String keyword, Page<News> pageable) {
        return newsMapper.search(pageable, keyword);
    }
    
    @Override
    public IPage<News> search(String title, Integer categoryId, 
                            LocalDateTime startDate, LocalDateTime endDate, 
                            Page<News> pageable) {
        return newsMapper.advancedSearch(pageable, title, categoryId, startDate, endDate);
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
    public Page<News> findAllWithImages(Page<News> pageable) {
        return newsMapper.selectAllWithImages(pageable);
    }
    
    @Override
    public IPage<News> findHotAndRecentNews(Page<News> pageable) {
        // 实现按照时间和热度交叉排序的新闻查询
        return newsMapper.selectHotAndRecentNews(pageable);
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
} 