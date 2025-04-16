package com.wiseflow.util;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




import org.springframework.http.*;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * 体育评论AI生成工具类
 */
@Component
public class SportCommentAiUtil {

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





    private static final Logger logger = LoggerFactory.getLogger(SportCommentAiUtil.class);
    private static final Random random = new Random();

    /**
     * 根据新闻标题和内容生成体育评论
     * 
     * @param title 新闻标题
     * @param sportType 体育类型
     * @return 生成的评论
     */
    public   String generateSportComment(String title,  String sportType) {
        logger.debug("开始为新闻生成体育评论，类型: {}", sportType);
        
        String prompt = String.format("你是一个体育评论专家。请根据以下新闻标题生成一条专业的%s评论:\n" +
                "标题: %s\n" +
                "要求:\n" +
                "1. 评论要专业、客观\n" +
                "2. 突出重点\n" +
                "3. 语言自然流畅\n" +
                "请直接给出评论内容，不要包含任何解释。", sportType, title);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(modelApiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            logger.error("调用AI接口生成评论失败", e);
        }
        
        return "这是一条AI生成的体育评论";
    }

    /**
     * 根据新闻标题和内容生成指定类型的体育评论
     * 
     * @param title 新闻标题
     * @param sportType 体育类型
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 生成的评论
     */
    public   String generateTypedSportComment(String title,  String sportType, int minLength, int maxLength) {
        logger.debug("开始生成指定长度的体育评论，类型: {}, 长度范围: {}-{}", sportType, minLength, maxLength);
        
        String prompt = String.format("你是一个体育评论专家。请根据以下新闻标题生成一条专业的%s评论:\n" +
                "标题: %s\n" +
                "要求:\n" +
                "1. 评论要专业、客观\n" +
                "2. 突出重点\n" +
                "3. 语言自然流畅\n" +
                "4. 评论长度在%d-%d字之间\n" +
                "请直接给出评论内容，不要包含任何解释。", sportType, title, minLength, maxLength);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(modelApiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            logger.error("调用AI接口生成评论失败", e);
        }

        return "这是一条AI生成的指定长度体育评论";
    }

    /**
     * 批量生成体育评论
     * 
     * @param title 新闻标题
     * @param count 生成数量
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @return 评论列表
     */
    public   List<String> batchGenerateSportComments(String title,   int count, int minLength, int maxLength) {
        logger.debug("开始批量生成体育评论，数量: {}", count);
        
        List<String> comments = new ArrayList<>();
        String prompt = String.format("你是一个体育评论专家。请根据以下新闻标题生成%d条不同的专业评论:\n" +
                "标题: %s\n" +
                "要求:\n" +
                "1. 评论要专业、客观\n" +
                "2. 突出重点\n" +
                "3. 语言自然流畅\n" +
                "4. 每条评论长度在%d-%d字之间\n" +
                "5. 评论内容要有差异性\n" +
                "请直接给出评论内容，每条评论占一行，不要包含任何解释。", count, title, minLength, maxLength);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(modelApiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    comments.addAll(Arrays.asList(content.split("\n")));
                }
            }
        } catch (Exception e) {
            logger.error("调用AI接口批量生成评论失败", e);
            comments = List.of("这是第一条AI评论", "这是第二条AI评论");
        }

        return comments;
    }
}