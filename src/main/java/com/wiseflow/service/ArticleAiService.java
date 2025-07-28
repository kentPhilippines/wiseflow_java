package com.wiseflow.service;

import com.wiseflow.model.ArticleAiTask;
import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.entity.SeoKeyword;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ArticleAiService {

    CompletableFuture<ArticleRewrite> processArticleWithKeywords(Long articleId, String title, String content, List<SeoKeyword> keywords);

    String processArticleTitle(Integer id);

    String processArticleContent(Integer id);
} 