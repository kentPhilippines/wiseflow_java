package com.wiseflow.util;

import com.wiseflow.entity.Article;
import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.entity.DomainConfig;
import com.wiseflow.entity.SeoKeyword;
import com.wiseflow.model.ArticleAiTask;
import com.wiseflow.service.ArticleAiService;
import com.wiseflow.service.ArticleService;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.service.SeoKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 文章工具类，封装文章改写相关功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleUtil {

    private final ArticleAiService articleAiService;
    private final ArticleService articleService;
    private final SeoKeywordService seoKeywordService;
    private final DomainConfigService domainConfigService;
    
    private static final long DEFAULT_TIMEOUT = 300; // 默认超时时间（秒）

    /**
     * 提交改写任务
     * 
     * @param articleId 文章ID
     * @param keywordIds 关键词ID列表（可选）
     * @param domain 域名（可选，如果提供则使用该域名下的所有关键词）
     * @return 任务ID
     */
    public String submitRewriteTask(Long articleId, List<Long> keywordIds, String domain) {
        log.info("提交文章改写任务: articleId={}, keywordIds={}, domain={}", articleId, keywordIds, domain);
        
        // 获取文章信息
        Article article = articleService.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在: " + articleId);
        }
        
        List<SeoKeyword> keywords = new ArrayList<>();
        
        // 如果提供了特定的关键词ID列表，则使用这些关键词
        if (keywordIds != null && !keywordIds.isEmpty()) {
            for (Long keywordId : keywordIds) {
                SeoKeyword keyword = seoKeywordService.findById(keywordId);
                if (keyword != null) {
                    keywords.add(keyword);
                }
            }
        } 
        // 如果提供了域名，则使用该域名下的所有关键词
        else if (domain != null && !domain.isEmpty()) {
            keywords = seoKeywordService.getKeywordsByDomain(domain);
        }
        
        if (keywords.isEmpty()) {
            throw new RuntimeException("未找到可用的关键词");
        }
        
        log.info("提交文章改写任务，使用关键词: {}", 
                keywords.stream().map(SeoKeyword::getKeyword).collect(Collectors.joining(", ")));
        
        // 提交改写任务
        String taskId = articleAiService.submitAiTask(articleId, article.getTitle(), article.getContent());
        
        // 在后台异步执行改写任务
        executeRewriteTask(taskId, article, keywords);
        
        return taskId;
    }
    
    /**
     * 执行改写任务
     * 
     * @param taskId 任务ID
     * @param article 文章对象
     * @param keywords 关键词列表
     */
    public void executeRewriteTask(String taskId, Article article, List<SeoKeyword> keywords) {
        log.info("开始执行文章改写任务: taskId={}, articleId={}", taskId, article.getId());
        
        // 异步执行改写任务
        CompletableFuture.runAsync(() -> {
            try {
                // 执行基于关键词的文章改写
                CompletableFuture<ArticleRewrite> future = articleAiService.processArticleWithKeywords(
                        article.getId(),
                        article.getTitle(),
                        article.getContent(),
                        keywords
                );
                
                // 等待改写结果
                ArticleRewrite rewrite = future.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
                
                if (rewrite != null) {
                    log.info("文章改写成功: taskId={}, articleId={}, originalityScore={}", 
                            taskId, article.getId(), rewrite.getOriginalityScore());
                } else {
                    log.warn("文章改写失败: taskId={}, articleId={}", taskId, article.getId());
                }
                
            } catch (InterruptedException e) {
                log.error("改写任务被中断: taskId={}, articleId={}", taskId, article.getId(), e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("改写任务执行异常: taskId={}, articleId={}", taskId, article.getId(), e);
            } catch (TimeoutException e) {
                log.error("改写任务超时: taskId={}, articleId={}", taskId, article.getId(), e);
            } catch (Exception e) {
                log.error("改写任务未知异常: taskId={}, articleId={}", taskId, article.getId(), e);
            }
        });
    }
    
    /**
     * 同步改写结果
     * 
     * @param articleId 文章ID
     * @param keywords 关键词列表
     * @param timeoutSeconds 超时时间（秒）
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticle(Long articleId, List<SeoKeyword> keywords, long timeoutSeconds) {
        log.info("同步改写文章: articleId={}, timeoutSeconds={}", articleId, timeoutSeconds);
        
        Article article = articleService.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在: " + articleId);
        }
        
        if (keywords == null || keywords.isEmpty()) {
            throw new RuntimeException("关键词列表不能为空");
        }
        
        try {
            // 执行基于关键词的文章改写，并同步等待结果
            CompletableFuture<ArticleRewrite> future = articleAiService.processArticleWithKeywords(
                    articleId,
                    article.getTitle(),
                    article.getContent(),
                    keywords
            );
            
            // 设置超时时间
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
            
        } catch (InterruptedException e) {
            log.error("同步改写被中断: articleId={}", articleId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("改写任务被中断", e);
        } catch (ExecutionException e) {
            log.error("同步改写执行异常: articleId={}", articleId, e);
            throw new RuntimeException("改写任务执行异常: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            log.error("同步改写超时: articleId={}, timeoutSeconds={}", articleId, timeoutSeconds, e);
            throw new RuntimeException("改写任务超时", e);
        }
    }
    
    /**
     * 同步改写结果（使用默认超时时间）
     * 
     * @param articleId 文章ID
     * @param keywords 关键词列表
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticle(Long articleId, List<SeoKeyword> keywords) {
        return syncRewriteArticle(articleId, keywords, DEFAULT_TIMEOUT);
    }
    
    /**
     * 根据域名同步改写文章
     * 
     * @param articleId 文章ID
     * @param domain 域名
     * @param timeoutSeconds 超时时间（秒）
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticleByDomain(Long articleId, String domain, long timeoutSeconds) {
        log.info("根据域名同步改写文章: articleId={}, domain={}, timeoutSeconds={}", 
                articleId, domain, timeoutSeconds);
        
        List<SeoKeyword> keywords = seoKeywordService.getKeywordsByDomain(domain);
        if (keywords.isEmpty()) {
            throw new RuntimeException("域名下未找到可用的关键词: " + domain);
        }
        
        log.info("使用域名{}下的{}个关键词改写文章", domain, keywords.size());
        return syncRewriteArticle(articleId, keywords, timeoutSeconds);
    }
    
    /**
     * 根据域名同步改写文章（使用默认超时时间）
     * 
     * @param articleId 文章ID
     * @param domain 域名
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticleByDomain(Long articleId, String domain) {
        return syncRewriteArticleByDomain(articleId, domain, DEFAULT_TIMEOUT);
    }

    /**
     * 根据域名配置对象改写文章
     * 
     * @param articleId 文章ID
     * @param domainConfig 域名配置对象
     * @param timeoutSeconds 超时时间（秒）
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticleByDomainConfig(Long articleId, DomainConfig domainConfig, long timeoutSeconds) {
        log.info("根据域名配置改写文章: articleId={}, domain={}, timeoutSeconds={}", 
                articleId, domainConfig.getDomain(), timeoutSeconds);
        
        // 获取文章信息
        Article article = articleService.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在: " + articleId);
        }
        
        // 1. 从域名配置中获取关键词
        List<String> keywordTexts = domainConfig.getKeywordsSeo();
        List<SeoKeyword> domainKeywords = domainConfig.getSeoKeywords();
        
        // 最终使用的关键词列表
        List<SeoKeyword> keywordsToUse = new ArrayList<>();
        
        // 2. 如果域名配置中有关联的SEO关键词对象，优先使用这些关键词
        if (domainKeywords != null && !domainKeywords.isEmpty()) {
            log.info("使用域名配置关联的SEO关键词: {}", 
                    domainKeywords.stream().map(SeoKeyword::getKeyword).collect(Collectors.joining(", ")));
            
            // 按照关键词类型和规则筛选
            for (SeoKeyword keyword : domainKeywords) {
                // 只使用启用的、适用于文章内容的关键词（useScene=1或3表示适用于文章内容）
                if (keyword.getEnabled() && (keyword.getUseScene() == 1 || keyword.getUseScene() == 3)) {
                    keywordsToUse.add(keyword);
                }
            }
        }
        
        // 3. 如果没有关联的SEO关键词对象或筛选后为空，则使用域名配置中的普通关键词
        if (keywordsToUse.isEmpty() && !keywordTexts.isEmpty()) {
            log.info("使用域名配置中的普通关键词: {}", String.join(", ", keywordTexts));
            
            // 为每个关键词文本创建一个简单的SeoKeyword对象
            for (String keywordText : keywordTexts) {
                SeoKeyword keyword = new SeoKeyword();
                keyword.setKeyword(keywordText);
                keyword.setEnabled(true);
                keyword.setMaxInsertions(3); // 默认最多插入3次
                keyword.setAllowTitle(true); // 默认允许在标题中使用
                keyword.setType(2); // 默认为长尾关键词
                keyword.setUseScene(1); // 默认用于文章内容
                
                keywordsToUse.add(keyword);
            }
        }
        
        // 4. 如果最终没有可用的关键词，抛出异常
        if (keywordsToUse.isEmpty()) {
            throw new RuntimeException("域名配置中未找到可用的关键词: " + domainConfig.getDomain());
        }
        
        log.info("最终使用{}个关键词改写文章: {}", keywordsToUse.size(), 
                keywordsToUse.stream().map(SeoKeyword::getKeyword).collect(Collectors.joining(", ")));
        
        // 5. 执行改写
        return syncRewriteArticle(articleId, keywordsToUse, timeoutSeconds);
    }
    
    /**
     * 根据域名配置对象改写文章（使用默认超时时间）
     * 
     * @param articleId 文章ID
     * @param domainConfig 域名配置对象
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticleByDomainConfig(Long articleId, DomainConfig domainConfig) {
        return syncRewriteArticleByDomainConfig(articleId, domainConfig, DEFAULT_TIMEOUT);
    }
    
    /**
     * 根据域名查找域名配置，然后改写文章
     * 
     * @param articleId 文章ID
     * @param domainName 域名名称
     * @param timeoutSeconds 超时时间（秒）
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticleByDomainName(Long articleId, String domainName, long timeoutSeconds) {
        log.info("根据域名名称改写文章: articleId={}, domainName={}", articleId, domainName);
        
        // 1. 根据域名名称查找域名配置
        DomainConfig domainConfig = domainConfigService.findByDomain(domainName)
            .orElseThrow(() -> new RuntimeException("未找到域名配置: " + domainName));
        
        // 2. 使用域名配置改写文章
        return syncRewriteArticleByDomainConfig(articleId, domainConfig, timeoutSeconds);
    }
    
    /**
     * 根据域名查找域名配置，然后改写文章（使用默认超时时间）
     * 
     * @param articleId 文章ID
     * @param domainName 域名名称
     * @return 改写后的文章
     */
    public ArticleRewrite syncRewriteArticleByDomainName(Long articleId, String domainName) {
        return syncRewriteArticleByDomainName(articleId, domainName, DEFAULT_TIMEOUT);
    }
    
    /**
     * 根据关键词规则优化改写文章
     * 
     * @param article 文章对象
     * @param keywords 关键词列表
     * @return 改写后的文章
     */
    public CompletableFuture<ArticleRewrite> applyKeywordRulesToArticle(Article article, List<SeoKeyword> keywords) {
        log.info("根据关键词规则优化改写文章: articleId={}, keywordsCount={}", article.getId(), keywords.size());
        
        // 根据关键词规则构建提示，包含使用频率、位置等规则
        StringBuilder keywordRules = new StringBuilder();
        keywordRules.append("关键词使用规则：\n");
        
        for (SeoKeyword keyword : keywords) {
            keywordRules.append("- 关键词: ").append(keyword.getKeyword()).append("\n");
            
            // 添加插入次数规则
            if (keyword.getMaxInsertions() != null) {
                keywordRules.append("  - 出现次数: ").append(keyword.getMaxInsertions()).append("次\n");
            } else {
                keywordRules.append("  - 出现次数: 1-3次\n");
            }
            
            // 添加标题规则
            if (keyword.getAllowTitle() != null && keyword.getAllowTitle()) {
                keywordRules.append("  - 允许在标题中使用\n");
            }
            
            // 添加类型信息
            if (keyword.getType() != null) {
                keywordRules.append("  - 类型: ").append(keyword.getType() == 1 ? "主关键词" : "长尾关键词").append("\n");
                
                // 主关键词优先级更高
                if (keyword.getType() == 1) {
                    keywordRules.append("  - 优先级: 高\n");
                }
            }
        }
        
        log.info("构建关键词规则:\n{}", keywordRules.toString());
        
        // 执行基于详细规则的文章改写
        return articleAiService.processArticleWithKeywords(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                keywords
        );
    }

    /**
     * 基于关键词规则构建自定义AI改写提示词
     * 
     * @param title 原文标题
     * @param content 原文内容
     * @param keywords 关键词列表
     * @return 包含标题提示和内容提示的数组
     */
    public String[] buildCustomPrompts(String title, String content, List<SeoKeyword> keywords) {
        log.info("构建自定义AI改写提示词，使用{}个关键词", keywords.size());
        
        // 提取关键词文本和规则
        StringBuilder keywordsList = new StringBuilder();
        List<String> mainKeywords = new ArrayList<>();
        List<String> longTailKeywords = new ArrayList<>();
        List<String> titleKeywords = new ArrayList<>();
        
        // 分类关键词
        for (SeoKeyword keyword : keywords) {
            String keywordText = keyword.getKeyword();
            
            // 按关键词类型分类
            if (keyword.getType() != null && keyword.getType() == 1) {
                mainKeywords.add(keywordText); // 主关键词
            } else {
                longTailKeywords.add(keywordText); // 长尾关键词
            }
            
            // 可用于标题的关键词
            if (keyword.getAllowTitle() != null && keyword.getAllowTitle()) {
                titleKeywords.add(keywordText);
            }
            
            // 构建关键词详情
            keywordsList.append("- ").append(keywordText);
            
            // 添加关键词规则
            if (keyword.getMaxInsertions() != null) {
                keywordsList.append(" (出现").append(keyword.getMaxInsertions()).append("次)");
            }
            
            if (keyword.getType() != null && keyword.getType() == 1) {
                keywordsList.append(" [主关键词]");
            }
            
            keywordsList.append("\n");
        }
        
        // 构建标题提示词
        String titlePrompt = "你是一个专业的文章标题优化专家。请改写以下标题，要求：\n" +
                "1. 保持专业性和可读性\n" +
                "2. 不要过分夸张\n" +
                "3. 长度适中\n" +
                "4. 突出文章重点\n";
        
        // 如果有可用于标题的关键词，添加到提示中
        if (!titleKeywords.isEmpty()) {
            titlePrompt += "5. 必须自然地在标题中包含以下至少一个关键词：" + 
                    String.join("、", titleKeywords) + "\n";
        }
        
        titlePrompt += "\n原标题：" + title + "\n请直接给出改写后的标题，不要包含任何解释。";
        
        // 构建内容提示词，整合关键词规则
        String contentPrompt = "你是一个专业的文章改写专家。请改写以下文章，要求：\n" +
                "1. 保持主要意思不变，但使用不同的表达方式重写\n" +
                "2. 提高原创性，避免简单的同义词替换\n" +
                "3. 保持专业性和可读性\n" +
                "4. 保持文章结构完整\n" +
                "5. 确保逻辑连贯\n" +
                "6. 保持文章排版设计\n" +
                "7. 图片引用不要改变\n" +
                "8. 不要改变文章的结构\n";
        
        // 添加关键词要求
        contentPrompt += "9. 必须在文章中自然地包含以下所有关键词：\n" + keywordsList.toString() + "\n";
        
        // 添加特殊规则
        if (!mainKeywords.isEmpty()) {
            contentPrompt += "10. 主关键词（标记为[主关键词]）是重点，必须确保在文章中突出显示\n";
        }
        
        if (!longTailKeywords.isEmpty()) {
            contentPrompt += "11. 长尾关键词可以在文章中适当分布，确保自然融入\n";
        }
        
        contentPrompt += "12. 关键词插入必须自然流畅，不影响阅读体验\n";
        contentPrompt += "13. 如果某个关键词已指定出现次数，请严格遵循\n";
        
        contentPrompt += "\n原文：" + content + "\n请直接给出改写后的文章，不要包含任何解释。";
        
        log.debug("标题提示词长度: {}", titlePrompt.length());
        log.debug("内容提示词长度: {}", contentPrompt.length());
        
        return new String[]{titlePrompt, contentPrompt};
    }
    
    /**
     * 使用定制化提示词请求AI改写文章
     * 
     * @param articleId 文章ID
     * @param keywords 关键词列表
     * @return 异步执行的改写任务
     */
    public CompletableFuture<ArticleRewrite> rewriteArticleWithCustomPrompts(Long articleId, List<SeoKeyword> keywords) {
        log.info("使用定制化提示词改写文章: articleId={}, keywordsCount={}", articleId, keywords.size());
        
        Article article = articleService.findById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在: " + articleId);
        }
        
        if (keywords == null || keywords.isEmpty()) {
            throw new RuntimeException("关键词列表不能为空");
        }
        
        // 构建定制化提示词
        String[] prompts = buildCustomPrompts(article.getTitle(), article.getContent(), keywords);
        String titlePrompt = prompts[0];
        String contentPrompt = prompts[1];
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始使用定制化提示词改写文章: articleId={}", articleId);
                
                // 改写标题
                log.info("使用定制化提示词改写标题");
                String newTitle = articleAiService.submitAiTask(articleId, titlePrompt, article.getContent());
                // 获取AI生成的标题结果（注意：这里简化处理了，实际应该从ArticleAiTask中获取结果）
                ArticleAiTask titleTask = articleAiService.getTaskStatus(newTitle);
                if (titleTask != null && titleTask.getStatus().equals("COMPLETED")) {
                    newTitle = titleTask.getTitle();
                } else {
                    // 如果没有成功获取AI改写标题，使用原标题
                    newTitle = article.getTitle();
                    log.warn("未能获取AI改写标题，使用原标题");
                }
                
                // 改写内容
                log.info("使用定制化提示词改写内容");
                // 使用已有的processArticleWithKeywords方法改写内容
                CompletableFuture<ArticleRewrite> future = articleAiService.processArticleWithKeywords(
                        articleId,
                        newTitle,
                        article.getContent(),
                        keywords
                );
                
                // 等待改写结果
                ArticleRewrite rewrite = future.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
                
                // 如果成功获取改写结果，返回该结果
                if (rewrite != null) {
                    log.info("文章改写成功: articleId={}, originalityScore={}", 
                            articleId, rewrite.getOriginalityScore());
                    return rewrite;
                } else {
                    throw new RuntimeException("文章改写失败，未能获取改写结果");
                }
                
            } catch (Exception e) {
                log.error("使用定制化提示词改写文章失败: articleId={}, error={}", articleId, e.getMessage(), e);
                throw new RuntimeException("改写文章失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 根据域名配置使用定制化提示词改写文章
     * 
     * @param articleId 文章ID
     * @param domainConfig 域名配置
     * @param timeoutSeconds 超时时间（秒）
     * @return 改写后的文章
     */
    public ArticleRewrite rewriteArticleWithDomainConfig(Long articleId, DomainConfig domainConfig, long timeoutSeconds) {
        log.info("根据域名配置使用定制化提示词改写文章: articleId={}, domain={}", 
                articleId, domainConfig.getDomain());
        
        // 获取关键词
        List<SeoKeyword> keywords = getKeywordsFromDomainConfig(domainConfig);
        
        try {
            // 执行改写并等待结果
            return rewriteArticleWithCustomPrompts(articleId, keywords).get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("改写任务被中断: articleId={}", articleId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("改写任务被中断", e);
        } catch (ExecutionException e) {
            log.error("改写任务执行异常: articleId={}", articleId, e);
            throw new RuntimeException("改写任务执行异常: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            log.error("改写任务超时: articleId={}, timeoutSeconds={}", articleId, timeoutSeconds, e);
            throw new RuntimeException("改写任务超时", e);
        }
    }
    
    /**
     * 从域名配置中提取可用的关键词
     */
    private List<SeoKeyword> getKeywordsFromDomainConfig(DomainConfig domainConfig) {
        // 最终使用的关键词列表
        List<SeoKeyword> keywordsToUse = new ArrayList<>();
        
        // 从域名配置中获取关键词
        List<String> keywordTexts = domainConfig.getKeywordsSeo();
        List<SeoKeyword> domainKeywords = domainConfig.getSeoKeywords();
        
        // 如果域名配置中有关联的SEO关键词对象，优先使用这些关键词
        if (domainKeywords != null && !domainKeywords.isEmpty()) {
            log.info("使用域名配置关联的SEO关键词: {}", 
                    domainKeywords.stream().map(SeoKeyword::getKeyword).collect(Collectors.joining(", ")));
            
            // 按照关键词类型和规则筛选
            for (SeoKeyword keyword : domainKeywords) {
                // 只使用启用的、适用于文章内容的关键词（useScene=1或3表示适用于文章内容）
                if (keyword.getEnabled() && (keyword.getUseScene() == 1 || keyword.getUseScene() == 3)) {
                    keywordsToUse.add(keyword);
                }
            }
        }
        
        // 如果没有关联的SEO关键词对象或筛选后为空，则使用域名配置中的普通关键词
        if (keywordsToUse.isEmpty() && !keywordTexts.isEmpty()) {
            log.info("使用域名配置中的普通关键词: {}", String.join(", ", keywordTexts));
            
            // 为每个关键词文本创建一个简单的SeoKeyword对象
            for (String keywordText : keywordTexts) {
                SeoKeyword keyword = new SeoKeyword();
                keyword.setKeyword(keywordText);
                keyword.setEnabled(true);
                keyword.setMaxInsertions(3); // 默认最多插入3次
                keyword.setAllowTitle(true); // 默认允许在标题中使用
                keyword.setType(2); // 默认为长尾关键词
                keyword.setUseScene(1); // 默认用于文章内容
                
                keywordsToUse.add(keyword);
            }
        }
        
        // 如果最终没有可用的关键词，抛出异常
        if (keywordsToUse.isEmpty()) {
            throw new RuntimeException("域名配置中未找到可用的关键词: " + domainConfig.getDomain());
        }
        
        return keywordsToUse;
    }
    
    /**
     * 根据域名配置使用定制化提示词改写文章（使用默认超时时间）
     * 
     * @param articleId 文章ID
     * @param domainConfig 域名配置
     * @return 改写后的文章
     */
    public ArticleRewrite rewriteArticleWithDomainConfig(Long articleId, DomainConfig domainConfig) {
        return rewriteArticleWithDomainConfig(articleId, domainConfig, DEFAULT_TIMEOUT);
    }
    
    /**
     * 根据域名名称使用定制化提示词改写文章
     * 
     * @param articleId 文章ID
     * @param domainName 域名名称
     * @param timeoutSeconds 超时时间（秒）
     * @return 改写后的文章
     */
    public ArticleRewrite rewriteArticleWithDomainName(Long articleId, String domainName, long timeoutSeconds) {
        log.info("根据域名名称使用定制化提示词改写文章: articleId={}, domainName={}", articleId, domainName);
        
        // 根据域名名称查找域名配置
        DomainConfig domainConfig = domainConfigService.findByDomain(domainName)
            .orElseThrow(() -> new RuntimeException("未找到域名配置: " + domainName));
        
        // 使用域名配置改写文章
        return rewriteArticleWithDomainConfig(articleId, domainConfig, timeoutSeconds);
    }
    
    /**
     * 根据域名名称使用定制化提示词改写文章（使用默认超时时间）
     * 
     * @param articleId 文章ID
     * @param domainName 域名名称
     * @return 改写后的文章
     */
    public ArticleRewrite rewriteArticleWithDomainName(Long articleId, String domainName) {
        return rewriteArticleWithDomainName(articleId, domainName, DEFAULT_TIMEOUT);
    }
}
