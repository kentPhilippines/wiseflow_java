package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wiseflow.entity.Article;
import com.wiseflow.mapper.ArticleMapper;
import com.wiseflow.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文章服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final ArticleMapper articleMapper;

    @Override
    public Article findById(Long id) {
        return articleMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Article saveArticle(Article article) {
        saveOrUpdate(article);
        log.info("保存文章成功: id={}, title={}", article.getId(), article.getTitle());
        return article;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        articleMapper.deleteById(id);
        log.info("删除文章成功: id={}", id);
    }

    @Override
    public List<Article> findAll() {
        return articleMapper.selectList(null);
    }

    @Override
    public List<Article> findByDomain(String domain) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getDomain, domain)
                .orderByDesc(Article::getCreateTime);
        return articleMapper.selectList(queryWrapper);
    }

    @Override
    public List<Article> findByCategoryId(Integer categoryId) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getCategoryId, categoryId)
                .orderByDesc(Article::getCreateTime);
        return articleMapper.selectList(queryWrapper);
    }
    
    @Override
    public List<Article> getArticlesToRewrite(String domain, int limit) {
        log.info("获取待改写文章: domain={}, limit={}", domain, limit);
        
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Article::getDomain, domain)
                .eq(Article::getIsOriginal, true) // 只获取原创文章
                .isNull(Article::getIsRewritten) // 未设置改写状态的
                .or()
                .eq(Article::getIsRewritten, false) // 或未改写的
                .orderByDesc(Article::getCreateTime) // 最新的优先
                .last("LIMIT " + limit); // 限制数量
        
        List<Article> articles = articleMapper.selectList(queryWrapper);
        log.info("找到{}篇待改写文章", articles.size());
        return articles;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticleRewriteStatus(Long articleId, boolean isRewritten) {
        log.info("更新文章改写状态: articleId={}, isRewritten={}", articleId, isRewritten);
        
        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, articleId)
                .set(Article::getIsRewritten, isRewritten);
        
        int rows = articleMapper.update(null, updateWrapper);
        if (rows > 0) {
            log.info("更新文章改写状态成功: articleId={}", articleId);
        } else {
            log.warn("更新文章改写状态失败，可能文章不存在: articleId={}", articleId);
        }
    }
} 