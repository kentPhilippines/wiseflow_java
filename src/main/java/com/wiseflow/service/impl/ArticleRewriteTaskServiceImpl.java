package com.wiseflow.service.impl;

import com.wiseflow.service.ArticleRewriteTaskService;
import com.wiseflow.service.ArticleAiService;
import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.entity.Article;
import com.wiseflow.entity.News;
import com.wiseflow.entity.NewsContent;
import com.wiseflow.entity.NewsImage;
import com.wiseflow.mapper.ArticleRewriteMapper;
import com.wiseflow.mapper.ArticleMapper;
import com.wiseflow.mapper.NewsMapper;
import com.wiseflow.mapper.NewsContentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleRewriteTaskServiceImpl implements ArticleRewriteTaskService {

    private final Map<String, String> taskStatusMap = new ConcurrentHashMap<>();

} 