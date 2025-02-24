package com.wiseflow.core.storage;

import com.wiseflow.entity.Article;
import java.util.List;

public interface Storage {
    void save(Article article);
    Article load(String id);
    List<Article> loadAll();
    void delete(Article article);
} 