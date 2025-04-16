package com.wiseflow.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

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

    public static void main(String[] args) {
        // 创建服务实例
        ArticleAiServiceImpl service = new ArticleAiServiceImpl(null, new RestTemplate());

        // 配置API参数
        service.modelApiUrl = "https://api.deepseek.com/v1/chat/completions";
        service.apiToken = "sk-36da0e78863c4f29bf1b109bb8b92ac3";
        service.modelName = "deepseek-chat";

        // 准备测试数据
        String testTitle = """
            标题提示: 你是一个专业的文章标题优化专家。请改写以下标题，要求：
                1. 保持专业性和可读性
                2. 不要过分夸张
                3. 长度适中
                4. 突出文章重点
                原标题：过去十年，12位英年早逝的明星，有人留下140亿遗产，有人仅28岁
                请直接给出改写后的标题，不要包含任何解释。
                """;
        String testContent = """
        内容提示: 你是一个专业的文章改写专家。请改写以下文章，要求：
        1. 保持主要意思不变，但使用不同的表达方式重写
        2. 提高原创性，避免简单的同义词替换
        3. 保持专业性和可读性
        4. 保持文章结构完整
        5. 确保逻辑连贯
        6. 保持文章排版设计
        7. 图片引用不要改变
        8. 不要改变文章的结构
        原文：<p id="3F1PSHGK">过去这段时间，已经有太多我们熟悉的名人离世。</p>
        <p id="3F1PSHGL">这其中既有备受观众喜爱的女演员郑佩佩，著名女作家琼瑶，也有主演过《英雄虎胆》的共和国电影功勋于洋。</p>
        <p class="f_center">
        <img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F06496cd2j00ssqw6q003hd0019700zkm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHGN">相比这些老一代的明星，还有一些年轻明星的离世，更让人震惊。</p>
        <p id="3F1PSHGO">他们都有着不小的知名度，年龄也不大，却在某一刻骤然离世，带给世界极大震惊。</p>
        <p id="3F1PSHGP">今天，皮哥就给大家盘点下，过去十年里，那些英年早逝的12位明星。</p>
        <p id="3F1PSHGQ">有些发生得太过虚幻，即使已经过去了数年，仍然让人感觉很不真实。</p>
        <p id="3F1PSHGR">第一位：<a target="_blank" href="https://ent.163.com/keywords/5/2/59270053/1.html">大S</a>（48岁）</p>
        <p id="3F1PSHGS">春节期间，大S和家人去日本旅游，期间因为流感导致了肺炎。</p>
        <p id="3F1PSHGT">再加上自身的基础病与免疫力不足，几乎是很短的时间内撒手人寰。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F0c07a4c4j00ssqw6p000xd000in008xm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="671" height="321" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHGV">而随着她的意外离世，S家与汪小菲这边的纠纷又再度喧嚣于网。</p>
        <p id="3F1PSHH0">比如所谓的"包机风波"、"<a target="_blank" href="https://ent.163.com/keywords/9/5/90574ea7/1.html">遗产</a>事件"等等。</p>
        <p id="3F1PSHH1">并且涉事其中的汪小菲、汪妈以及大S的现任丈夫具俊晔等都有被网友质疑。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F8cce1237j00ssqw6p000ud000ij0098m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="667" height="332" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHH3">如今，大S去世已经满一个月了，自在日本火化后，其骨灰仍旧放置在家中。</p>
        <p id="3F1PSHH4">这几天，具俊晔被爆出在找墓地，但随着他一改之前不要遗产的说法，变为与孩子一起平分遗产后，关于墓地的消息又再度消失了。</p>
        <p id="3F1PSHH5">倒是S妈这两天的发文有点"内涵"，疑似指责具俊晔。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F1030052dj00ssqw6p000ed000il009tm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="669" height="353" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHH7">毕竟大S结婚三年，身体没养好，还断送了她们家的一半家财。</p>
        <p id="3F1PSHH8">第二位：<span search-type="3" search-href="newsapp://nc/search?entry=articleKeyword&amp;word=%E6%96%B9%E5%A4%A7%E5%90%8C">方大同</span>（41岁）</p>
        <p id="3F1PSHH9">与大S一样，方大同走得也是突然。</p>
        <p id="3F1PSHHA">早在很久之前，他就因为身体原因，选择了短暂性的退圈。</p>
        <p id="3F1PSHHB">从他发出来的短视频能看出，他的身体状况确实出现了问题，整个人相比之前消瘦了很多。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F44ac9790j00ssqw6q003cd00111017km.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHHD">此外，他也因为曾经高强度工作的原因，导致过"爆肺"，呼吸机能本就有问题。</p>
        <p id="3F1PSHHE">再加上他是一个长期素食主义者，自身的免疫系统能力并不强。</p>
        <p id="3F1PSHHF">因此，自他在2021年感染疾病后，一直在与病魔抗争，但可惜，他没能康复过来。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ff1dfa24fj00ssqw6q0026d000w5018fm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHHH">方大同离世的消息，没有在第一时间传出。</p>
        <p id="3F1PSHHI">不过在他离世的次日，好友薛凯琪在音乐节中的表现就有端倪。</p>
        <p id="3F1PSHHJ">在唱歌中，她的眼泪止不住地落下，还哽咽地说，"只要我们还在。"</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F6f1d8557j00ssqw6p000hd000ik0090m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="668" height="324" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHHL">目前，方大同已经在云南被火化。</p>
        <p id="3F1PSHHM">这短暂的一生中，他拥有了喜欢的音乐与喜欢的人，这或许对他就是最好的慰藉。</p>
        <p id="3F1PSHHN">第三位：<span search-type="3" search-href="newsapp://nc/search?entry=articleKeyword&amp;word=%E5%91%A8%E6%B5%B7%E5%AA%9A">周海媚</span>（57岁）</p>
        <p id="3F1PSHHO">2023年年末，周海媚因病去世，在这之前几乎没有任何征兆。</p>
        <p id="3F1PSHHP">回看她的一生，其实挺潇洒的，典型的射手女，敢爱敢恨。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ff5662983j00ssqw6p000hd000hs00bum.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="640" height="426" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHHR">就在她离世的前几天，她还发了一个庆祝自己过生日的视频。</p>
        <p id="3F1PSHHS">画面中，她仍旧面容姣好，笑颜灿烂。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F435e470aj00ssqw6p001cd000fy00l6m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="574" height="762" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHHU">其实，网上对她的诸多争议，大抵归置于她的情感。</p>
        <p id="3F1PSHHV">不过，于她而言，如果有喜欢的人就一定会去追求，即便不被祝福。</p>
        <p class="f_center"><img src="http://dingyue.ws.126.net/2025/0307/ca4226f7g00ssqw6q019fd000gc007qm.gif"><br><br></p>
        <p id="3F1PSHI1">如果没有，她也佛系得很，甚至觉得男人在身边会碍手碍脚。</p>
        <p id="3F1PSHI2">好酷一姐们！</p>
        <p class="f_center"><img src="http://dingyue.ws.126.net/2025/0307/37bdcf9ag00ssqw6p00gxd000gc007gm.gif"><br><br></p>
        <p id="3F1PSHI4">当然，面对有些网友的刻意"嘲讽"，她也不惯着。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ffb8a1242j00ssqw6p000ld000dq00bqm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="494" height="422" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHI6">生命的最后阶段，她被病痛折磨，但仍旧乐观。</p>
        <p id="3F1PSHI7">并且，她会告诉每一个人，<strong>"我真的好喜欢我自己呀"</strong>。</p>
        <p id="3F1PSHI8">只是现实往往是残酷的，她离世后，其母很快出售了她在北京的家，陪伴她很久的狗狗也被送人。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F9606761ej00ssqw6p001rd000ib00i3m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="659" height="651" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHIA">一年多的时间，娱乐圈似乎已经忘了她，好在，还有粉丝记得她。</p>
        <p id="3F1PSHIB">第四位：<span search-type="3" search-href="newsapp://nc/search?entry=articleKeyword&amp;word=%E9%AB%98%E4%BB%A5%E7%BF%94"><a target="_blank" href="https://ent.163.com/keywords/9/d/9ad84ee57fd4/1.html">高以翔</a></span>（35岁）</p>
        <p id="3F1PSHIC">高以翔是在录制节目时出现意外，最终猝死身亡。</p>
        <p id="3F1PSHID">曾有知情人透露，当时高以翔在中途表示过自己快受不了了，需要喝水休息。</p>
        <p id="3F1PSHIE">随后不久，他就倒地失去意识。</p>
        <p id="3F1PSHIF">虽然医护人员进行了抢救，但还是无力回天。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F9cb8bd98j00ssqw6p0017d000ir00d9m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="675" height="477" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHIH">高以翔出事后，这档节目立刻就遭受各方抨击。</p>
        <p id="3F1PSHII">很多专业人士比如邹市明、李小鹏等，都表示录制这个节目非常累，甚至需要吸氧。</p>
        <p id="3F1PSHIJ">这也不禁让人揣测究竟是嘉宾的生命安全重要，还是那些博眼球的套路重要？</p>
        <p id="3F1PSHIK">如今，高以翔离世已快五年。</p>
        <p id="3F1PSHIL">粉丝与其家人们都不曾忘记他，而他的生前女友也不时在社交平台缅怀。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fac3dc2f1j00ssqw6p001od000in00bdm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="671" height="409" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHIN">第五位：<span search-type="3" search-href="newsapp://nc/search?entry=articleKeyword&amp;word=%E4%B9%94%E4%BB%BB%E6%A2%81"><a target="_blank" href="https://ent.163.com/keywords/4/5/4e544efb6881/1.html">乔任梁</a></span>（28岁）</p>
        <p id="3F1PSHIO">关于乔任梁的死，网络上一直都有"阴谋论"的说法。</p>
        <p id="3F1PSHIP">不过，在去年的时候，乔任梁的父母出面澄清，表示乔任梁的确是因为抑郁症去世。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F18f6f199j00ssqw6p000qd000h7009mm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="619" height="346" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHIR">并且，其父母还在采访中给出解释。</p>
        <p id="3F1PSHIS">因为乔任梁生前身上时常就有伤，并且他经常还把自己关在房间里，拉上窗帘，不开灯，仅仅是呆坐在那里。</p>
        <p id="3F1PSHIT">还有在乔任梁自杀的前一年，他的社交动态也经常表现出不对劲。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Feec0f585j00ssqw6p0019d000hc00aam.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="624" height="370" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHIV">其实，当时他的抑郁症可能就已经非常严重了，只是外界并不了解。</p>
        <p id="3F1PSHJ0">如今，他已去世多年，仍旧有一部分网友妖魔化这件事。</p>
        <p id="3F1PSHJ1">甚至，就连好不容易走出失去儿子阴影的老两口，也在做视频被一些人辱骂。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fe80a45c2j00ssqw6p000wd000em00a7m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="526" height="367" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fed0b00b5j00ssqw6p0012d000gh00dtm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="593" height="497" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fe9898e8fj00ssqw6p001td000o600d0m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="870" height="468" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHJ7">唉！何至于此！</p>
        <p id="3F1PSHJ8">希望这样的伤害以后不要有了。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ffef56d2ej00ssqw6q0034d0011m00gqm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHJA">第六位：赵英俊（43岁）</p>
        <p id="3F1PSHJB">赵英俊，留给这个世界的最后一句话是，"希望你们别那么快将我遗忘。"</p>
        <p id="3F1PSHJC">而今，四年多了，你还记得他吗？</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fecc90509j00ssqw6q0038d000uw010gm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHJE">讲真，他绝对是一个音乐才子，那些我们熟悉的爆款院线电影，主题曲很多都是出自他之手。</p>
        <p id="3F1PSHJF">我们最熟悉的便是他的遗作《送你一朵小红花》。</p>
        <p id="3F1PSHJG">在他的遗书中，我们能感受到，他对自己的人生原本有着非常美好的规划。</p>
        <p id="3F1PSHJH">比如娶心爱的人为妻，生小孩，带着爸妈去海边。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F61d33f8fj00ssqw6q0028d0010w00l0m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHJJ">可惜，这一切都被癌症化为幻影。</p>
        <p id="3F1PSHJK">多年的漂泊经历，让他有了用音乐表达芸芸众生的资本，但这些却是他用健康换取的。</p>
        <p id="3F1PSHJL">当然，在生命的最后时刻、被病痛折磨的他，仍旧没忘记给别人希望。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F278ea641j00ssqw6p000kd000gb007om.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="587" height="276" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHJN">第七位：<span search-type="3" search-href="newsapp://nc/search?entry=articleKeyword&amp;word=%E4%BA%8E%E6%9C%88%E4%BB%99">于月仙</span>（50岁）</p>
        <p id="3F1PSHJO">于月仙是在内蒙古突遭车祸，导致身亡的。</p>
        <p id="3F1PSHJP">而在事发前几天，她刚拍完《乡村爱情15》。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F1b6aeb88j00ssqw6p001md000ff00cvm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="555" height="463" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHJR">对于于月仙，大家对她的印象或许就只有"谢大姐"这个角色。</p>
        <p id="3F1PSHJS">但其实，她年轻时候的颜值也很仙，比如在《西游记后传》中的她。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fbf0780afj00ssqw6p000ed000ff009em.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="555" height="338" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHJU">靠着多年的扎实表演，她也慢慢有了知名度。</p>
        <p id="3F1PSHJV">不过2005年后，于月仙主要就是在表姐夫赵本山的公司拍戏，演《乡村爱情》。</p>
        <p id="3F1PSHK0">其实这些年，于月仙已经逐渐开始跳脱出"谢大脚"这一定型式人物。</p>
        <p id="3F1PSHK1">她尝试自己当导演，参加表演综艺，拓宽自己的角色类型。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F582778b8j00ssqw6q002nd001cc00kkm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHK3">可惜，就在一切正向好时，她出了车祸。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F5f6853bfj00ssqw6p001ld000nv00lkm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="859" height="776" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHK5">后面《乡村爱情》系列，还是在角色上，给了观众一个交代——</p>
        <p id="3F1PSHK6">谢大脚的母亲身体不好，她得待在娘家长期照顾，所以暂时不会回来了。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ff5dd4cf9j00ssqw6q002sd000o900u3m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="873" height="1083" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHK8">王长贵的扮演者王小宝也退出了后面的续集计划：谢大脚不在了，我这个角色毫无意义。</p>
        <p id="3F1PSHK9">而就在她出事不久，丈夫张学松还被一些媒体扒出点赞美女跳舞视频，甚至被抨击抢夺于月仙的五亿遗产。</p>
        <p id="3F1PSHKA">这让张学松很是气愤，不过他并没有做过多口舌之争。</p>
        <p id="3F1PSHKB">一年后，他在社交平台公布了车祸进展，肇事者被抓，而他也承诺会照顾好妻子的母亲与弟弟。</p>
        <p id="3F1PSHKC">第八位：蓝洁瑛（55岁）</p>
        <p id="3F1PSHKD">蓝洁瑛年轻时候的颜值不用多说。</p>
        <p id="3F1PSHKE">出道就与圈中多位大佬合作。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F8bf26f93j00ssqw6p000pd000ik00bom.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="668" height="420" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHKG">但她的命似乎真的不好，出道即巅峰，之后离开TVB后，也没有很好的演出机会。</p>
        <p id="3F1PSHKH">事业低迷之余，她的感情之路也是一波三折，先是交往三年的男友邓启扬自杀，后面又被圈内大佬欺辱。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F464082abj00ssqw6p000ud000i9006ym.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="657" height="250" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHKJ">而宠爱她的父母也相继离世，甚至她还被好友骗财。</p>
        <p id="3F1PSHKK">2006年，她宣布破产，还患上了抑郁症。</p>
        <p id="3F1PSHKL">2018年，她于家中猝死，两三天后才被发现。</p>
        <p id="3F1PSHKM">可叹，红颜薄命！</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F7274843fj00ssqw6p0015d000ib00b6m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="659" height="402" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHKO">第九位：姚贝娜（33岁）</p>
        <p id="3F1PSHKP">姚贝娜出生在音乐世家，自身的音乐天赋也好。</p>
        <p id="3F1PSHKQ">从中国音乐学院毕业后，她还参加了青歌赛，并且是参赛选手中唯一一个被打满分的选手。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F084b2a74j00ssqw6p000ed000ft009om.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="569" height="348" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHKS">但不幸的是，她在事业的发展期中，被查出肿瘤，好在她的手术非常成功。</p>
        <p id="3F1PSHKT">在康复后，她不仅为爆款剧《甄嬛传》唱了两首歌，还参加了《中国好声音》，有了不小的名气。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F7b0208f1j00ssqw6q001td000xg00i4m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fba94190aj00ssqw6q002od001c000tvm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHL1">但在2014年底，她的病情突然复发，并且恶化得很快，最后她因乳腺癌去世。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F73933bdbj00ssqw6p0012d000vc00h2m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHL3">在她去世后，父母遵守她的遗愿，将其眼角膜捐献给了一个孩子。</p>
        <p id="3F1PSHL4">第十位：<a target="_blank" href="https://ent.163.com/keywords/6/4/674e739f/1.html">李玟</a>（48岁）</p>
        <p id="3F1PSHL5">说实话，将李玟与抑郁症放在一起，怎么都不搭。</p>
        <p id="3F1PSHL6">但真相就是如此。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F0c961a39j00ssqw6p002dd000lg00u0m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="772" height="1080" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F9aa7ada9j00ssqw6p000gd000gu007km.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="606" height="272" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHLA">回头看她生前的那几年，确实很难。</p>
        <p id="3F1PSHLB">丈夫是出轨惯犯，她多次试管又流产，可她总是在努力治愈自己，也在宽慰别人。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ffe017096j00ssqw6p000ud000ix007fm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="681" height="267" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHLD">可她也是一个人，总有脆弱的时候。</p>
        <p id="3F1PSHLE">更何况长期的压力，让她已经患上了抑郁症，在生命的最后几天，她选择了轻生。</p>
        <p id="3F1PSHLF">最后，因救治无效，离开了人世。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F1f6a0fb7j00ssqw6q0030d000n400usm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="832" height="1108" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHLH">当然，李玟在离世前是有准备遗嘱的，财产全给母亲。</p>
        <p id="3F1PSHLI">但其丈夫似乎并不甘心。</p>
        <p id="3F1PSHLJ">不过，这位的躁动似乎没有什么用，毕竟有遗嘱在。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F2e0adeeaj00ssqw6q001pd000ih00bsm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="665" height="424" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHLL">只是让李玟在离世后，还陷入这样的污糟事中，着实让人心疼。</p>
        <p id="3F1PSHLM">第十一位：中山美穗（54岁）</p>
        <p id="3F1PSHLN">看过日本经典电影《情书》的观众，都知道她。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fe4e002dcj00ssqw6q000td000ho009xm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="636" height="357" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHLP">她从出道开始，就有着"第一美人"的称号，因此她年轻时候的作品大多是主咖，星途很好。</p>
        <p id="3F1PSHLQ">可她的情感之路就没有这么顺遂了。</p>
        <p id="3F1PSHLR">早年她与有着"日本MJ"称号的田原俊彦相爱，不过这份美好恋情并没有持续太久。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ffcae060bj00ssqw6q000id000i20095m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="650" height="329" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHLT">后来她遇到了一个设计师，没想到也是渣男。</p>
        <p id="3F1PSHLU">之后闪婚一位作家，两人的婚姻也出现了问题。</p>
        <p id="3F1PSHLV">最近这一段感情是与一位音乐人，但没想到她被对方当街打骂。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F992748caj00ssqw6q000jd000i700aym.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="655" height="394" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHM1">近些年，她的作品少了很多，更多的是网友对她外貌的苛责。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ffeb71baaj00ssqw6q000ud000h500afm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="617" height="375" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ff673786fj00ssqw6q000cd000gk0029m.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="596" height="81" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHM5">而她在离世前，原本是准备出席一场圣诞音乐会的，但没想到失联。</p>
        <p id="3F1PSHM6">被发现时，她已经在家中去世多时。</p>
        <p id="3F1PSHM7">第十二位：科比（41岁）</p>
        <p id="3F1PSHM8">对于无数篮球迷来说，2020年的1月26日无疑是一个悲伤的日子。</p>
        <p id="3F1PSHM9">当天，科比与其13岁的二女儿乘坐直升机去往训练营，结果遭遇大雾，最终飞机撞向山体，机上人全部遇难。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Fbee5dda9j00ssqw6q005qd0013400zkm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHMB">出事时，科比已经退役，正在从篮球运动员的身份，向演艺圈转型。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2Ffc3090aaj00ssqw6q002ud000rs00rsm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="1000" height="1000" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHMD">他将自己的退役信《亲爱的篮球》改编成了动画短片，还获得了奥斯卡。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F56d8a6e6j00ssqw6q002td0018900tim.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHMF">对于科比，除了他在NBA谱写的传奇史诗外，还有他与妻子的爱情。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F23ae665cj00ssqw6q0057d001sf00zkm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHMH">婚姻十九年，他们有四个女儿，虽然他们的关系也曾亮过红灯。</p>
        <p id="3F1PSHMI">但在科比的辉煌与低谷时，陪伴在其身边的一直都是瓦妮莎。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F278b8a16j00ssqw6q0012d000h300aom.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg" width="615" height="384" onload="this.removeAttribute('width'); this.removeAttribute('height'); this.removeAttribute('onload');"><br><br></p>
        <p id="3F1PSHMK">科比离世后，为其家人留下了近140亿人民币的遗产。</p>
        <p id="3F1PSHML">但正是因为这些，瓦妮莎也一度被质疑，甚至有人拿她与友人亲密照说事。</p>
        <p id="3F1PSHMM">可事实上，失去丈夫与女儿，瓦妮莎无疑是痛苦的。</p>
        <p id="3F1PSHMN">三四年的时间，她的体重暴涨到了230斤。</p>
        <p id="3F1PSHMO">而在经历了各种求爱与复杂的官司后，她与女儿们的生活也渐渐回归平淡。</p>
        <p class="f_center"><img src="https://nimg.ws.126.net/?url=http%3A%2F%2Fdingyue.ws.126.net%2F2025%2F0307%2F08435f33j00ssqw6q005ld001hs00zkm.jpg&amp;thumbnail=660x2147483647&amp;quality=80&amp;type=jpg"><br><br></p>
        <p id="3F1PSHMQ">逝者长已矣，生者且如斯。</p>
        <p id="3F1PSHMR">不管你是成就非凡的明星，还是为柴米油盐奔波的普通人，你永远不知道意外会何时降临。</p>
        <p id="3F1PSHMS">唯一能做的，就是过好每一天，陪伴好自己的家人。</p>
        <p id="3F1PSHMU">文/皮皮电影编辑部：安言</p>
        <p id="3F1PSHMV">©原创丨文章著作权：皮皮电影（ppdianying）</p>
        <p id="3F1PSHN0">未经授权请勿进行任何形式的转载</p>
        请直接给出改写后的文章，不要包含任何解释。
         """;

        log.info(" 进行改写");
        try {
            // 提交AI改写任务
            Map<String, Object> params = new HashMap<>();
            params.put("model", service.modelName);
            params.put("messages", List.of(Map.of(
                    "role", "user",
                    "content", testContent
            )));
            params.put("temperature", 0.7);

            HttpResponse execute = HttpUtil.createPost(service.modelApiUrl)
                    .header("Authorization", "Bearer " + service.apiToken)
                    .body(JSONUtil.toJsonStr(params))
                    .execute();
                   String body =  execute.body();
                log.info(body, args);
        } catch (Exception e) {
            log.error("测试过程发生异常: ", e);
        }
    }
}