package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wiseflow.entity.Article;
import com.wiseflow.mapper.ArticleMapper;
import com.wiseflow.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public List<Article> getArticlesByDomain(String domain) {
        return articleMapper.selectList(
            new QueryWrapper<Article>()
                .eq("status", 1)  // 假设1表示已发布状态
                .orderByDesc("create_time")
        );
    }
} 