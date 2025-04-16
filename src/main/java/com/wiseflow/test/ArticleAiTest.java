package com.wiseflow.test;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ArticleAiTest {
    private static final String API_URL = "https://api.siliconflow.cn/v1/chat/completions";
    private static final String API_TOKEN = "sk-scdcoeidzeqdqifkzyvtxoxddhkmsqpkikurptoqlcxxsivg";
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_DELAY = 10000; // 10秒
    private static final String MODEL_NAME = "THUDM/glm-4-9b-chat";
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
        "5. 确保逻辑连贯\n\n" +
        "原文：%s\n" +
        "请直接给出改写后的文章，不要包含任何解释。";

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        
        System.out.println("=== SiliconFlow API 测试开始 ===\n");
        
        // 1. 测试模型基本响应
        testBasicResponse(restTemplate);
        
        // 2. 测试文章改写
        testArticleRewriting(restTemplate);
        
        // 3. 测试特殊场景
        testSpecialCases(restTemplate);
        
        // 4. 测试并发性能
        testConcurrentPerformance(restTemplate);
        
        // 5. 测试原创度计算
        testOriginalityScore(restTemplate);
        
        System.out.println("\n=== 测试完成 ===");
    }

    private static void testBasicResponse(RestTemplate restTemplate) {
        System.out.println("=== 测试1：基本响应测试 ===");
        
        try {
            System.out.println("发送简单问候...");
            String response = callModel(restTemplate, "你好，请做个自我介绍。");
            System.out.println("响应内容：" + response);
            System.out.println("基本响应测试成功！\n");
        } catch (Exception e) {
            System.out.println("基本响应测试失败：" + e.getMessage() + "\n");
        }
    }

    private static void testArticleRewriting(RestTemplate restTemplate) {
        System.out.println("=== 测试2：文章改写测试 ===");
        
        // 测试标题改写
        System.out.println("\n2.1 标题改写测试");
        String[] titles = {
            "人工智能技术在医疗领域的应用与发展",
            "互联网教育平台的机遇与挑战",
            "新能源汽车市场的现状分析",
            "区块链技术如何改变金融行业",
            "5G技术对未来通信的影响"
        };
        
        for (String title : titles) {
            System.out.println("\n测试标题：" + title);
            try {
                String prompt = String.format(TITLE_REWRITE_PROMPT, title);
                String newTitle = callModel(restTemplate, prompt);
                System.out.println("改写结果：" + newTitle);
            } catch (Exception e) {
                System.out.println("标题改写失败：" + e.getMessage());
            }
        }

        // 测试不同长度文章改写
        System.out.println("\n2.2 文章改写测试");
        
        // 短文本测试
        String shortArticle = "人工智能正在改变我们的生活。";
        testContentRewrite(restTemplate, "短文本", shortArticle);

        // 中等长度文本测试
        String mediumArticle = "人工智能技术在医疗领域的应用日益广泛。从辅助诊断到智能医疗设备，AI正在改变传统医疗模式。" +
            "机器学习算法能够分析大量医疗数据，帮助医生做出更准确的诊断。";
        testContentRewrite(restTemplate, "中等长度文本", mediumArticle);

        // 长文本测试
        String longArticle = "人工智能技术在医疗领域的应用日益广泛。从辅助诊断到智能医疗设备，AI正在改变传统医疗模式。" +
            "机器学习算法能够分析大量医疗数据，帮助医生做出更准确的诊断。深度学习技术在医学影像识别方面表现出色，" +
            "可以快速准确地识别X光片、CT等影像中的异常。这些技术的应用不仅提高了诊断效率，也降低了医疗成本。" +
            "未来，随着技术的进步，AI在医疗领域的应用将更加普及，为人类健康带来更多福祉。";
        testContentRewrite(restTemplate, "长文本", longArticle);
    }

    private static void testContentRewrite(RestTemplate restTemplate, String testName, String content) {
        System.out.println("\n测试：" + testName);
        System.out.println("原文：" + content);
        System.out.println("长度：" + content.length() + " 字符");
        System.out.println("中文字符数：" + countChineseChars(content));
        
        long startTime = System.currentTimeMillis();
        try {
            String prompt = String.format(CONTENT_REWRITE_PROMPT, content);
            String rewritten = callModel(restTemplate, prompt);
            long endTime = System.currentTimeMillis();
            
            System.out.println("改写结果：" + rewritten);
            System.out.println("改写后长度：" + rewritten.length() + " 字符");
            System.out.println("改写后中文字符数：" + countChineseChars(rewritten));
            System.out.println("耗时：" + (endTime - startTime) + "ms");
            
            // 计算原创度
            int originalityScore = calculateOriginalityScore(content, rewritten);
            System.out.println("原创度评分：" + originalityScore);
        } catch (Exception e) {
            System.out.println("改写失败：" + e.getMessage());
        }
    }

    private static void testSpecialCases(RestTemplate restTemplate) {
        System.out.println("=== 测试3：特殊场景测试 ===");
        
        // 测试包含特殊字符的文本
        String specialCharsText = "AI技术发展迅速！@#￥%……&*（）——+【】《》？，。/";
        testContentRewrite(restTemplate, "特殊字符文本", specialCharsText);
        
        // 测试包含英文的混合文本
        String mixedText = "AI(Artificial Intelligence)人工智能正在revolutionize改变各个industry行业。";
        testContentRewrite(restTemplate, "中英混合文本", mixedText);
        
        // 测试专业术语文本
        String technicalText = "深度学习中的CNN（卷积神经网络）和RNN（循环神经网络）是重要的基础架构。";
        testContentRewrite(restTemplate, "专业术语文本", technicalText);
        
        // 测试HTML标签文本
        String htmlText = "<p>人工智能技术</p><div>正在改变世界</div>";
        testContentRewrite(restTemplate, "HTML标签文本", htmlText);
    }

    private static void testConcurrentPerformance(RestTemplate restTemplate) {
        System.out.println("=== 测试4：并发性能测试 ===");
        
        int numThreads = 3;
        int requestsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        String title = "测试标题" + threadId + "-" + j;
                        try {
                            String prompt = String.format(TITLE_REWRITE_PROMPT, title);
                            String response = callModel(restTemplate, prompt);
                            System.out.println("线程" + threadId + "请求" + (j + 1) + "成功：" + response);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            System.out.println("线程" + threadId + "请求" + (j + 1) + "失败：" + e.getMessage());
                            failCount.incrementAndGet();
                        }
                        Thread.sleep(3000); // 间隔3秒发送下一个请求
                    }
                } catch (Exception e) {
                    System.out.println("线程" + threadId + "执行异常：" + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("并发测试被中断");
        }
        
        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000.0;
        
        System.out.println("\n并发测试结果：");
        System.out.println("总请求数：" + (numThreads * requestsPerThread));
        System.out.println("成功请求：" + successCount.get());
        System.out.println("失败请求：" + failCount.get());
        System.out.println("总耗时：" + totalTime + "秒");
        System.out.println("平均响应时间：" + (totalTime / (numThreads * requestsPerThread)) + "秒");
        
        executor.shutdown();
    }

    private static void testOriginalityScore(RestTemplate restTemplate) {
        System.out.println("=== 测试5：原创度计算测试 ===");
        
        String originalText = "人工智能正在改变我们的生活方式和工作方式。";
        System.out.println("原文：" + originalText);
        
        try {
            String prompt = String.format(CONTENT_REWRITE_PROMPT, originalText);
            String rewritten = callModel(restTemplate, prompt);
            System.out.println("改写结果：" + rewritten);
            
            int score = calculateOriginalityScore(originalText, rewritten);
            System.out.println("原创度评分：" + score);
            
            // 测试相似度计算
            System.out.println("字符重叠率：" + calculateCharacterOverlap(originalText, rewritten) + "%");
            System.out.println("长度差异率：" + calculateLengthDifference(originalText, rewritten) + "%");
        } catch (Exception e) {
            System.out.println("原创度测试失败：" + e.getMessage());
        }
    }

    private static String callModel(RestTemplate restTemplate, String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + API_TOKEN);
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", prompt));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_NAME);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);
            requestBody.put("stream", false);
            requestBody.put("top_p", 0.7);
            requestBody.put("frequency_penalty", 0.5);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(API_URL, request, Map.class);
            
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null && message.containsKey("content")) {
                        return (String) message.get("content");
                    }
                }
            }
            
            return "无法解析响应数据";
            
        } catch (Exception e) {
            throw new RuntimeException("调用失败: " + e.getMessage());
        }
    }

    private static int calculateOriginalityScore(String originalContent, String newContent) {
        if (originalContent == null || newContent == null || 
            originalContent.isEmpty() || newContent.isEmpty()) {
            return 0;
        }

        // 计算中文字符的差异度
        int originalChineseCount = countChineseChars(originalContent);
        int newChineseCount = countChineseChars(newContent);
        
        // 计算字数差异率
        double lengthScore = Math.abs(newChineseCount - originalChineseCount) * 100.0 / originalChineseCount;
        
        // 计算内容差异
        int commonChars = 0;
        for (int i = 0; i < Math.min(originalContent.length(), newContent.length()); i++) {
            if (originalContent.charAt(i) == newContent.charAt(i)) {
                commonChars++;
            }
        }
        
        double similarityScore = (1 - (double) commonChars / originalContent.length()) * 100;
        
        // 综合评分
        return (int) ((lengthScore + similarityScore) / 2);
    }

    private static int countChineseChars(String text) {
        int count = 0;
        Matcher matcher = CHINESE_PATTERN.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static double calculateCharacterOverlap(String original, String rewritten) {
        int commonChars = 0;
        for (int i = 0; i < Math.min(original.length(), rewritten.length()); i++) {
            if (original.charAt(i) == rewritten.charAt(i)) {
                commonChars++;
            }
        }
        return (double) commonChars * 100 / original.length();
    }

    private static double calculateLengthDifference(String original, String rewritten) {
        return Math.abs(original.length() - rewritten.length()) * 100.0 / original.length();
    }
} 