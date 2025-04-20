package com.wiseflow.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.wiseflow.entity.SeoKeyword;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.wiseflow.service.ArticleAiService;
import com.wiseflow.model.ArticleAiTask;
import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.mapper.ArticleRewriteMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleAiServiceImpl implements ArticleAiService {

    private final Map<String, ArticleAiTask> taskMap = new ConcurrentHashMap<>();
    private final ArticleRewriteMapper articleRewriteMapper;
    private final RestTemplate restTemplate;

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY = 5000;
    private static final int MAX_WAIT_TIME = 60000;
    private static final int CONNECT_TIMEOUT = 600000;
    private static final int READ_TIMEOUT = 600000;
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    private static final String TITLE_REWRITE_PROMPT = "你是一个专业的文章标题优化专家。请改写以下标题，要求：\n" +
            "1. 保持专业性和可读性\n" +
            "2. 不要过分夸张\n" +
            "3. 长度适中\n" +
            "4. 突出文章重点\n\n" +
            "原标题：%s\n" +
            "请直接给出改写后的标题，不要包含任何解释。";

    private static final String CONTENT_REWRITE_PROMPT = "你是一个专业的文章改写专家。请改写以下文章，要求：\n" +
            "1. 保持主要意思不变，但使用不同的表达方式重写\n" +
            "2. 提高原创性，避免简单的同义词替换\n" +
            "3. 保持专业性和可读性\n" +
            "4. 保持文章结构完整\n" +
            "5. 确保逻辑连贯\n" +
            "6. 保持文章排版设计\n" +
            "7. 图片引用不要改变\n" +
            "8. 不要改变文章的结构\n" +
            "原文：%s\n" +
            "请直接给出改写后的文章，不要包含任何解释。";

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String modelApiUrl;

    @Value("${deepseek.api.token:sk-36da0e78863c4f29bf1b109bb8b92ac3}")
    private String apiToken;

    @Value("${article.ai.process.cron:0/30 * * * * ?}")
    private String processCron;

    @Value("${article.ai.model.name:deepseek-chat}")
    private String modelName;

    public ArticleAiServiceImpl(ArticleRewriteMapper articleRewriteMapper, RestTemplate restTemplate) {
        this.articleRewriteMapper = articleRewriteMapper;

        // 配置 RestTemplate 的超时时间
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        RestTemplate customRestTemplate = new RestTemplate(requestFactory);
        this.restTemplate = customRestTemplate;
    }
 
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    
    @Override
    public String submitAiTask(Long articleId, String title, String content) {
        log.info("提交AI改写任务: articleId={}", articleId);

        String taskId = UUID.randomUUID().toString();
        ArticleAiTask task = new ArticleAiTask();
        task.setTaskId(taskId);
        task.setArticleId(articleId);
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setContent(content);
        task.setTitle(title);
        taskMap.put(taskId, task);

        return taskId;
    }

    @Override
    public ArticleAiTask getTaskStatus(String taskId) {
        return taskMap.get(taskId);
    }
 //   @Scheduled(cron = "${article.ai.process.cron:0/15 * * * * ?}")
    public void processPendingArticles() {
        try {
            // 检查当前时间是否在优惠时段
            LocalTime now = LocalTime.now();
            LocalTime startTime = LocalTime.of(0, 30);
            LocalTime endTime = LocalTime.of(8, 30);

            // 判断是否在优惠时段内
            boolean isDiscountTime = false;
            if (now.isAfter(startTime) && now.isBefore(endTime)) {
                isDiscountTime = true;
            }

            if (isDiscountTime) {
                // 优惠时段使用6个线程并行处理
                for (int i = 0; i < 6; i++) {
                    ArticleAiTask task = taskMap.values().stream()
                        .filter(t -> "PENDING".equals(t.getStatus()))
                        .findFirst()
                        .orElse(null);
                    
                    if (task == null) {
                        log.debug("没有待处理的任务");
                        return;
                    }

                    // 使用CAS操作更新任务状态，防止重复处理
                    if (taskMap.replace(task.getTaskId(), task, 
                        updateTaskStatus(task, "PROCESSING"))) {
                        log.info("任务{}开始处理", task.getTaskId());
                        executorService.submit(() -> processNextPendingArticle(task));
                    } else {
                        log.debug("任务{}已被其他线程处理", task.getTaskId());
                    }
                }
            } else {
                // 非优惠时段单线程处理
                ArticleAiTask task = taskMap.values().stream()
                    .filter(t -> "PENDING".equals(t.getStatus()))
                    .findFirst()
                    .orElse(null);
                
                if (task == null) {
                    log.debug("没有待处理的任务");
                    return;
                }

                // 使用CAS操作更新任务状态，防止重复处理
                if (taskMap.replace(task.getTaskId(), task, 
                    updateTaskStatus(task, "PROCESSING"))) {
                    log.info("任务{}开始处理", task.getTaskId());
                    processNextPendingArticle(task);
                } else {
                    log.debug("任务{}已被其他线程处理", task.getTaskId());
                }
            }
        } catch(Exception e) {
            log.error("处理待改写文章任务异常: {}", e.getMessage(), e);
        }
    }
  
    @Override
    public String submitTask(Long articleId, String title, String content) {
        return null;
    }

    @Override
    public String getTaskResult(String taskId) {
        return null;
    }


  
    public void processNextPendingArticle(ArticleAiTask task) {
        try {


            log.info("开始处理任务: taskId={}, articleId={}", task.getTaskId(), task.getArticleId());

            try {
                // 改写标题
                log.info("开始改写标题: taskId={}", task.getTaskId());
                String titlePrompt = String.format(TITLE_REWRITE_PROMPT, task.getTitle());
                log.info("标题提示: {}", titlePrompt);
                String newTitle = callModelApiWithRetry(titlePrompt);

                // 改写内容
                log.info("开始改写内容: taskId={}", task.getTaskId());
                String contentPrompt = String.format(CONTENT_REWRITE_PROMPT, task.getContent());
                log.info("内容提示: {}", contentPrompt);
                String newContent = callModelApiWithRetry(contentPrompt);

                // 清理和格式化结果
                newTitle = cleanAndFormatText(newTitle);
                log.info("改写后的标题: {}", newTitle);
                newContent = cleanAndFormatText(newContent);
                log.info("改写后的内容: {}", newContent);

                // 计算原创度
                int originalityScore = calculateOriginalityScore(task.getContent(), newContent);
                log.info("文章改写完成: taskId={}, originalityScore={}", task.getTaskId(), originalityScore);

                // 只有成功改写的文章才保存到数据库
                if (originalityScore != 0) { // 设置一个原创度阈值
                    ArticleRewrite rewrite = new ArticleRewrite();
                    rewrite.setOriginalArticleId(task.getArticleId());
                    rewrite.setTitle(newTitle);
                    rewrite.setContent(newContent);
                    rewrite.setStatus("COMPLETED");
                    rewrite.setOriginalityScore(originalityScore);
                    rewrite.setCreateTime(LocalDateTime.now());
                    rewrite.setUpdateTime(LocalDateTime.now());
                    articleRewriteMapper.insert(rewrite);

                    task.setStatus("COMPLETED");
                    task.setResult(newContent);
                } else {
                    log.warn("文章原创度不足: taskId={}, originalityScore={}", task.getTaskId(), originalityScore);
                    task.setStatus("FAILED");
                    task.setErrorMessage("文章原创度不足: " + originalityScore);
                }

            } catch (Exception e) {
                log.error("处理任务失败: taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
                task.setStatus("FAILED");
                task.setErrorMessage(e.getMessage());
            }

            task.setUpdateTime(LocalDateTime.now());
            // 清理已完成或失败的任务
            if ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus())) {
                taskMap.remove(task.getTaskId());
            }

        } catch (Exception e) {
            log.error("处理待改写文章任务异常: {}", e.getMessage(), e);
        }

        
    }

    @Override
    @Async
    public CompletableFuture<ArticleRewrite> processArticleAsync(Long articleId, String title, String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始异步处理文章: articleId={}", articleId);

                // 改写标题
                log.info("开始改写标题: articleId={}", articleId);
                String titlePrompt = String.format(TITLE_REWRITE_PROMPT, title);
                String newTitle = callModelApiWithRetry(titlePrompt);

                // 改写内容
                log.info("开始改写内容: articleId={}", articleId);
                String contentPrompt = String.format(CONTENT_REWRITE_PROMPT, content);
                String newContent = callModelApiWithRetry(contentPrompt);

                // 清理和格式化结果
                newTitle = cleanAndFormatText(newTitle);
                newContent = cleanAndFormatText(newContent);

                // 计算原创度
                int originalityScore = calculateOriginalityScore(content, newContent);

                // 只有原创度达标的文章才保存
                if (originalityScore != 0) {
                    ArticleRewrite rewrite = new ArticleRewrite();
                    rewrite.setOriginalArticleId(articleId);
                    rewrite.setTitle(newTitle);
                    rewrite.setContent(newContent);
                    rewrite.setStatus("COMPLETED");
                    rewrite.setOriginalityScore(originalityScore);
                    rewrite.setCreateTime(LocalDateTime.now());
                    rewrite.setUpdateTime(LocalDateTime.now());
                    articleRewriteMapper.insert(rewrite);
                    return rewrite;
                } else {
                    log.warn("文章原创度不足: articleId={}, originalityScore={}", articleId, originalityScore);
                    return null;
                }

            } catch (Exception e) {
                log.error("异步处理文章失败: articleId={}, error={}", articleId, e.getMessage(), e);
                throw new RuntimeException("处理文章失败: " + e.getMessage());
            }
        });
    }

    /**
     * 根据关键词规则改写文章
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @param keywords 需要包含的关键词列表
     * @return 改写后的文章
     */
    @Override
    @Async
    public CompletableFuture<ArticleRewrite> processArticleWithKeywords(Long articleId, String title, String content, List<SeoKeyword> keywords) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("开始基于关键词改写文章: articleId={}, keywords={}", articleId, 
                    keywords.stream().map(SeoKeyword::getKeyword).collect(Collectors.joining(",")));
                
                // 构建包含关键词的提示
                StringBuilder keywordsList = new StringBuilder();
                for (SeoKeyword keyword : keywords) {
                    keywordsList.append("- ").append(keyword.getKeyword()).append("\n");
                }
                
                // 改写标题，确保包含关键词
                String keywordTitlePrompt = String.format(
                    "你是一个专业的文章标题优化专家。请改写以下标题，要求：\n" +
                    "1. 保持专业性和可读性\n" +
                    "2. 不要过分夸张\n" +
                    "3. 长度适中\n" +
                    "4. 突出文章重点\n" +
                    "5. 自然地包含以下关键词中的至少一个（如果原标题已包含则可保留）：\n%s\n" +
                    "原标题：%s\n" +
                    "请直接给出改写后的标题，不要包含任何解释。", 
                    keywordsList.toString(), title);
                
                log.info("标题提示: {}", keywordTitlePrompt);
                String newTitle = callModelApiWithRetry(keywordTitlePrompt);
                
                // 改写内容，确保包含关键词
                String keywordContentPrompt = String.format(
                    "你是一个专业的文章改写专家。请改写以下文章，要求：\n" +
                    "1. 保持主要意思不变，但使用不同的表达方式重写\n" +
                    "2. 提高原创性，避免简单的同义词替换\n" +
                    "3. 保持专业性和可读性\n" +
                    "4. 保持文章结构完整\n" +
                    "5. 确保逻辑连贯\n" +
                    "6. 保持文章排版设计\n" +
                    "7. 图片引用不要改变\n" +
                    "8. 不要改变文章的结构\n" +
                    "9. 必须自然地在文章中包含以下所有关键词（每个关键词至少出现1次，最多3次）：\n%s\n" +
                    "原文：%s\n" +
                    "请直接给出改写后的文章，不要包含任何解释。", 
                    keywordsList.toString(), content);
                
                log.info("内容提示: {}", keywordContentPrompt);
                String newContent = callModelApiWithRetry(keywordContentPrompt);
                
                // 清理和格式化结果
                newTitle = cleanAndFormatText(newTitle);
                log.info("改写后的标题: {}", newTitle);
                newContent = cleanAndFormatText(newContent);
                log.info("改写后的内容: {}", newContent);
                
                // 确认所有关键词都包含在内容中
                boolean allKeywordsIncluded = true;
                StringBuilder missingKeywords = new StringBuilder();
                
                for (SeoKeyword keyword : keywords) {
                    if (!newContent.contains(keyword.getKeyword())) {
                        allKeywordsIncluded = false;
                        missingKeywords.append(keyword.getKeyword()).append(", ");
                        
                        // 尝试插入缺失的关键词
                        newContent = insertMissingKeyword(newContent, keyword.getKeyword());
                    }
                }
                
                if (!allKeywordsIncluded) {
                    log.warn("改写后仍有关键词缺失，已尝试手动插入: {}", missingKeywords.toString());
                }
                
                // 计算原创度
                int originalityScore = calculateOriginalityScore(content, newContent);
                log.info("文章改写完成: articleId={}, originalityScore={}", articleId, originalityScore);
                
                // 只有原创度达标的文章才保存
                if (originalityScore >= 30) { // 设置一个原创度阈值
                    ArticleRewrite rewrite = new ArticleRewrite();
                    rewrite.setOriginalArticleId(articleId);
                    rewrite.setTitle(newTitle);
                    rewrite.setContent(newContent);
                    rewrite.setStatus("COMPLETED");
                    rewrite.setOriginalityScore(originalityScore);
                    rewrite.setCreateTime(LocalDateTime.now());
                    rewrite.setUpdateTime(LocalDateTime.now());
                    articleRewriteMapper.insert(rewrite);
                    return rewrite;
                } else {
                    log.warn("文章原创度不足: articleId={}, originalityScore={}", articleId, originalityScore);
                    return null;
                }
                
            } catch (Exception e) {
                log.error("基于关键词改写文章失败: articleId={}, error={}", articleId, e.getMessage(), e);
                return null;
            }
        });
    }
    
    /**
     * 在文章中插入缺失的关键词
     * @param content 文章内容
     * @param keyword 关键词
     * @return 插入关键词后的内容
     */
    private String insertMissingKeyword(String content, String keyword) {
        try {
            // 解析HTML内容
            Document doc = Jsoup.parse(content);
            Elements paragraphs = doc.select("p");
            
            if (!paragraphs.isEmpty()) {
                // 选择一个较长的段落插入关键词
                Element targetParagraph = null;
                int maxLength = 0;
                
                for (Element p : paragraphs) {
                    String text = p.text();
                    if (text.length() > maxLength && text.length() > 20) {
                        maxLength = text.length();
                        targetParagraph = p;
                    }
                }
                
                if (targetParagraph != null) {
                    String text = targetParagraph.text();
                    // 在句子末尾或适当位置插入关键词
                    int insertPoint = text.indexOf("。");
                    if (insertPoint != -1 && insertPoint < text.length() - 1) {
                        String newText = text.substring(0, insertPoint + 1) + 
                                        "关于" + keyword + "，" + 
                                        text.substring(insertPoint + 1);
                        targetParagraph.text(newText);
                        return doc.body().html();
                    }
                }
            }
            
            // 如果无法在段落中插入，则在文章结尾添加一个新段落
            Element body = doc.body();
            body.append("<p>总的来说，" + keyword + "在这一领域具有重要意义，值得我们深入研究。</p>");
            
            return doc.body().html();
        } catch (Exception e) {
            log.error("插入缺失关键词失败: {}", e.getMessage());
            // 如果解析失败，直接在末尾添加
            return content + "\n<p>总的来说，" + keyword + "在这一领域具有重要意义，值得我们深入研究。</p>";
        }
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

    private String callModelApi(String prompt) {
        log.debug("调用模型API, prompt长度: {}", prompt.length());
        
        try {
            // 构建请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("model", modelName);
            params.put("messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt
            )));
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

    private String cleanAndFormatText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 只移除多余的空白字符,保留HTML标签
        return text.replaceAll("\\s+", " ").trim();
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

    private ArticleAiTask updateTaskStatus(ArticleAiTask task, String status) {
        ArticleAiTask newTask = new ArticleAiTask();
        newTask.setTaskId(task.getTaskId());
        newTask.setArticleId(task.getArticleId());
        newTask.setTitle(task.getTitle());
        newTask.setContent(task.getContent());
        newTask.setStatus(status);
        newTask.setCreateTime(task.getCreateTime());
        newTask.setUpdateTime(LocalDateTime.now());
        return newTask;
    }

}