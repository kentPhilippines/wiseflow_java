package com.wiseflow.util;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wiseflow.entity.*;
import com.wiseflow.mapper.ArticleMapper;
import com.wiseflow.mapper.NewsContentMapper;
import com.wiseflow.mapper.NewsMapper;
import com.wiseflow.model.ArticleAiTask;
import com.wiseflow.service.ArticleAiService;
import com.wiseflow.service.ArticleService;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.service.SeoKeywordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.wiseflow.mapper.ArticleRewriteMapper;

/**
 * 文章工具类，封装文章改写相关功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleUtil {
    private final ArticleAiService articleAiService;
    private final ArticleRewriteMapper articleRewriteMapper;
    private final ArticleMapper articleMapper;
    private final NewsMapper newsMapper;
    private final NewsContentMapper newsContentMapper;

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 5000;
    private static final int MAX_WAIT_TIME = 60000;
    private static final int CONNECT_TIMEOUT = 600000;
    private static final int READ_TIMEOUT = 600000;
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    private final ArticleService articleService;
    private final SeoKeywordService seoKeywordService;
    private final DomainConfigService domainConfigService;
    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String modelApiUrl;

    @Value("${deepseek.api.token:sk-36da0e78863c4f29bf1b109bb8b92ac3}")
    private String apiToken;

    @Value("${article.ai.process.cron:0/30 * * * * ?}")
    private String processCron;

    @Value("${article.ai.model.name:deepseek-reasoner}")
    private String modelName;
    private static final long DEFAULT_TIMEOUT = 300; // 默认超时时间（秒）

    /**
     * 基于关键词规则构建自定义AI改写提示词
     *
     * @param title       原文标题
     * @param content     原文内容
     * @param keywords    关键词改写规则
     * @param keywordsSeo 关键词列表
     * @return 包含标题提示和内容提示的数组
     */
    public String[] buildCustomPrompts(String title, String content, List<SeoKeyword> keywords,
            List<String> keywordsSeo) {
        log.info("构建自定义AI改写提示词，使用{}个关键词", keywords.size());

        // 提取关键词文本和规则
        StringBuilder keywordsList = new StringBuilder();
        List<String> mainKeywords = new ArrayList<>();
        List<String> longTailKeywords = new ArrayList<>();
        List<String> titleKeywords = new ArrayList<>();
        for (String keyword : keywordsSeo) {
            keywords.stream().forEach(k -> {
                k.setKeyword(keyword);
            });
        }
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
            // 添加SEO关键词标记
            if (keywordsSeo != null && keywordsSeo.contains(keywordText)) {
                keywordsList.append(" [SEO关键词]");
            }
            if (keyword.getUseScene() == 3) {
                keywordsList.append(" [描文本]");
            }
            if (keyword.getUseScene() == 2) {
                keywordsList.append(" [长尾关键词]");
            }
            if (keyword.getUseScene() == 1) {
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
        contentPrompt += "9. 必须在文章中自然地包含以下至少3个关键词：\n" + keywordsList.toString() + "\n";

        // 添加特殊规则
        contentPrompt += "10. 主关键词（标记为[主关键词]）是重点，必须确保在文章中突出显示\n";
        contentPrompt += "11. 长尾关键词可以在文章中适当分布，确保自然融入\n";
        contentPrompt += "12. 描文本（标记为[描文本]）是重点，必须确保在文章中突出显示\n";

        contentPrompt += "13. 关键词插入必须自然流畅，不影响阅读体验\n";
        contentPrompt += "14. 如果某个关键词已指定出现次数，请严格遵循\n";
        contentPrompt += "15. 文章中必须包含描文本（标记为[描文本]）\n";
        contentPrompt += "16. 文章必须符合seo优化规则，便于搜索引擎收录和站点排名\n";
        contentPrompt += "\n原文：" + content + "\n请直接给出改写后的文章，不要包含任何解释。";

        log.debug("标题提示词长度: {}", titlePrompt.length());
        log.debug("内容提示词长度: {}", contentPrompt.length());
        return new String[] { titlePrompt, contentPrompt };
    }

    /**
     * 根据关键词改写文章
     *
     * @param articleId       文章ID
     * @param title           文章标题
     * @param keywordsSeo     关键词列表
     * @param originalContent 原文内容
     * @param keywords        关键词改写规则
     * @return 改写后的文章
     */
    public ArticleRewrite rewriteArticle(Long articleId, String title, List<String> keywordsSeo, String originalContent,
            List<SeoKeyword> keywords) {
        log.info("根据关键词改写文章: articleId={}, title={}, keywordsCount={}", articleId, title, keywords.size());
        if (keywords == null || keywords.isEmpty()) {
            throw new RuntimeException("关键词列表不能为空");
        }
        if (keywordsSeo == null || keywordsSeo.isEmpty()) {
            throw new RuntimeException("关键词列表不能为空");
        }
        if (originalContent == null || originalContent.isEmpty()) {
            throw new RuntimeException("原文内容不能为空");
        }
        if (title == null || title.isEmpty()) {
            throw new RuntimeException("标题不能为空");
        }
        if (articleId == null) {
            throw new RuntimeException("文章ID不能为空");
        }
        ArticleRewrite ar = articleRewriteMapper
                .selectOne(new LambdaQueryWrapper<ArticleRewrite>()
                        .eq(ArticleRewrite::getOriginalArticleId, articleId));
        if (ar != null) {
            log.info("文章已改写或进行中");
            if (ar.getStatus().equals("ARCHIVED")) {
                // 如果文章已改写或进行中，则返回null
                newsMapper.update(new LambdaUpdateWrapper<News>().eq(News::getId, articleId)
                        .set(News::getRewriteStatus, "COMPLETED")
                        .set(News::getUpdateTime, LocalDateTime.now())
                        .set(News::getNeedRewrite, false));
                return null;
            } else {
                return ar;
            }
        }
        try {
            // 构建定制化提示词
            String[] prompts = buildCustomPrompts(title, originalContent, keywords, keywordsSeo);
            String titlePrompt = prompts[0];
            String contentPrompt = prompts[1];

            // 改写标题
            log.info("使用定制化提示词改写标题");
            ArticleRewrite articleRewrite = new ArticleRewrite();
            articleRewrite.setOriginalArticleId(articleId);
            articleRewrite.setTitle(titlePrompt);
            articleRewrite.setContent(contentPrompt);
            articleRewrite.setOriginalityScore(0);
            articleRewrite.setStatus("PENDING");
            articleRewrite.setCreateTime(LocalDateTime.now());
            articleRewrite.setUpdateTime(LocalDateTime.now());
            articleRewriteMapper.insert(articleRewrite);
            return articleRewrite;
        } catch (Exception e) {
            log.error("根据关键词改写文章失败: articleId={}, title={}, keywordsCount={}", articleId, title, keywords.size(), e);
            throw new RuntimeException("根据关键词改写文章失败: " + e.getMessage(), e);
        }
    }

    public void processNextPendingArticle(ArticleRewrite task) {
        try {

            log.info("开始处理任务: taskId={}, articleId={}", task.getId(), task.getOriginalArticleId());

            try {
                // 改写标题
                log.info("开始改写标题: taskId={}", task.getId());
                String titlePrompt = task.getTitle();
                log.info("标题提示: {}", titlePrompt);
                String newTitle = callModelApiWithRetry(titlePrompt);

                // 改写内容
                log.info("开始改写内容: taskId={}", task.getId());
                String contentPrompt = task.getContent();
                log.info("内容提示: {}", contentPrompt);
                String newContent = callModelApiWithRetry(contentPrompt);

                // 清理和格式化结果
                newTitle = cleanAndFormatText(newTitle);
                log.info("改写后的标题: {}", newTitle);
                newContent = cleanAndFormatText(newContent);
                log.info("改写后的内容: {}", newContent);

                // 只有成功改写的文章才保存到数据库
                task.setStatus("COMPLETED");
                task.setUpdateTime(LocalDateTime.now());
                articleRewriteMapper.updateById(task);
                // 创建新的文章记录
                News originalNews = newsMapper.selectById(task.getOriginalArticleId().intValue());
                Article newArticle = new Article();
                newArticle.setTitle(newTitle);
                newArticle.setContent(newContent);
                newArticle.setCategoryId(originalNews.getCategoryId());
                newArticle.setCategoryName(originalNews.getCategoryName());
                newArticle.setSource("(AI改写)");
                newArticle.setAuthor(originalNews.getAuthor());
                newArticle.setUrl(originalNews.getUrl());
                newArticle.setPublishTime(originalNews.getPublishTime());
                newArticle.setCreateTime(LocalDateTime.now());
                newArticle.setUpdateTime(LocalDateTime.now());
                newArticle.setOriginalArticleId(task.getOriginalArticleId()); // 关联原新闻ID
                articleMapper.insert(newArticle);

            } catch (Exception e) {
                log.error("处理任务失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
            }

            task.setUpdateTime(LocalDateTime.now());
            // 清理已完成或失败的任务
            if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
            }

        } catch (Exception e) {
            log.error("处理待改写文章任务异常: {}", e.getMessage(), e);
        }

    }

    public void handleCompletedTask() {
        log.info("开始处理已完成的改写任务...");

        // 查询一条已完成的任务
        LambdaQueryWrapper<ArticleRewrite> query = new LambdaQueryWrapper<>();
        query.eq(ArticleRewrite::getStatus, "COMPLETED")
                .orderByAsc(ArticleRewrite::getCreateTime);

        List<ArticleRewrite> tasks = articleRewriteMapper.selectList(query);
        if (tasks.isEmpty()) {
            return;
        }
        for (ArticleRewrite task : tasks) {
            try {
                // 获取原新闻
                News originalNews = newsMapper.selectById(task.getOriginalArticleId().intValue());
                if (originalNews == null) {
                    log.error("原新闻不存在: {}", task.getOriginalArticleId());
                    return;
                }

                NewsContent originalContent = newsContentMapper.selectById(originalNews.getId());
                if (originalContent == null) {
                    log.error("原新闻内容不存在: {}", originalNews.getId());
                    return;
                }

                // 更新原新闻状态
                originalNews.setRewriteStatus("COMPLETED");
                newsMapper.updateById(originalNews);

                // 更新任务状态
                task.setStatus("ARCHIVED");
                articleRewriteMapper.updateById(task);

            } catch (Exception e) {
                log.error("处理已完成任务失败, ID: " + task.getOriginalArticleId(), e);
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
                articleRewriteMapper.updateById(task);

                // 更新原新闻状态为失败
                News news = new News();
                news.setId(task.getOriginalArticleId().intValue());
                news.setRewriteStatus("FAILED");
                newsMapper.updateById(news);
            }
        }
    }

    private String cleanAndFormatText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private String callModelApiWithRetry(String prompt) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                return callModelApi(prompt);
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    throw e;
                }
                log.warn("调用模型API失败，将在{}ms后重试: {}", RETRY_DELAY, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程被中断", ie);
                }
            }
        }
        throw new RuntimeException("达到最大重试次数");
    }

    private Integer calculateOriginalityScore(String originalContent, String newContent) {
        if (originalContent == null || newContent == null ||
                originalContent.isEmpty() || newContent.isEmpty()) {
            return 0;
        }
        // 计算中文字符数
        int originalChineseCount = countChineseCharacters(originalContent);
        int newChineseCount = countChineseCharacters(newContent);
        // 计算长度差异率
        double lengthDiffRate = Math.abs(newChineseCount - originalChineseCount) / (double) originalChineseCount;
        // 计算重叠率
        int overlapCount = calculateOverlap(originalContent, newContent);
        double overlapRate = overlapCount / (double) originalChineseCount;
        // 计算最终得分
        int lengthScore = lengthDiffRate <= 0.2 ? 40 : (lengthDiffRate <= 0.4 ? 30 : 20);
        int overlapScore = (int) ((1 - overlapRate) * 60);
        return Math.min(100, lengthScore + overlapScore);
    }

    private String callModelApi(String prompt) {
        log.debug("调用模型API, prompt长度: {}", prompt.length());

        try {
            // 构建请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("model", modelName);
            params.put("messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt)));
            params.put("temperature", 0.7);
            params.put("max_tokens", 8192);
            params.put("stream", false);
            params.put("top_p", 0.7);

            log.debug("发送请求到模型API: {}", modelApiUrl);

            // 使用 HttpUtil 发送请求
            HttpResponse response = HttpUtil.createPost(modelApiUrl)
                    .header("Authorization", "Bearer " + apiToken)
                    .body(JSONUtil.toJsonStr(params))
                    .execute();

            String responseBody = response.body();
            log.debug("API响应: {}", responseBody);

            // 解析响应
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            if (jsonResponse.containsKey("choices")) {
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (!choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    if (message != null && message.containsKey("content")) {
                        return message.getStr("content");
                    }
                }
            }

            throw new RuntimeException("无法解析响应数据: " + responseBody);

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("调用模型API失败: {}", errorMsg);
            throw new RuntimeException("调用模型API失败: " + errorMsg);
        }
    }

    private int countChineseCharacters(String text) {
        Matcher matcher = CHINESE_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private int calculateOverlap(String original, String newText) {
        // 将文本分成字符数组
        char[] originalChars = original.toCharArray();
        char[] newChars = newText.toCharArray();

        int overlapCount = 0;
        int windowSize = 3; // 使用3个字符的滑动窗口

        for (int i = 0; i <= originalChars.length - windowSize; i++) {
            String window = new String(originalChars, i, windowSize);
            if (newText.contains(window)) {
                overlapCount++;
            }
        }

        return overlapCount;
    }

}
