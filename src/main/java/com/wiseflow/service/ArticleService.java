package com.wiseflow.service;

import com.wiseflow.entity.Article;
import java.util.List;

public interface ArticleService {
    List<Article> getArticlesByDomain(String domain);
} 