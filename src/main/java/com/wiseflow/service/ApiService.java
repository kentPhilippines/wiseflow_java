package com.wiseflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API服务类，封装对内部API的调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiService {

    private final RestTemplate restTemplate;
    
    @Value("${wiseflow.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;
    
    /**
     * 获取新闻列表
     * 
     * @param categoryName 分类名称，可选
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 新闻列表
     */
    public List<Map<String, Object>> getNewsList(String categoryName, int pageNum, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/api/news/list")
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize);
                
        if (categoryName != null && !categoryName.isEmpty()) {
            builder.queryParam("categoryName", categoryName);
        }
        
        String url = builder.toUriString();
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data.containsKey("list")) {
                    return (List<Map<String, Object>>) data.get("list");
                }
            }
            
            log.error("获取新闻列表失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用新闻列表API失败: {}", url, e);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 获取新闻详情
     * 
     * @param newsId 新闻ID
     * @return 新闻详情
     */
    public Map<String, Object> getNewsDetail(Integer newsId) {
        if (newsId == null) {
            return Collections.emptyMap();
        }
        
        String url = apiBaseUrl + "/api/news/detail/" + newsId;
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                return (Map<String, Object>) responseBody.get("data");
            }
            
            log.error("获取新闻详情失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用新闻详情API失败: {}", newsId, e);
        }
        
        return Collections.emptyMap();
    }
    
    /**
     * 获取热门新闻
     * 
     * @param limit 限制数量，默认5
     * @return 热门新闻列表
     */
    public List<Map<String, Object>> getHotNews(int limit) {
        String url = apiBaseUrl + "/api/news/hot?limit=" + limit;
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                return (List<Map<String, Object>>) responseBody.get("data");
            }
            
            log.error("获取热门新闻失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用热门新闻API失败", e);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 获取分类列表
     * 
     * @return 分类列表
     */
    public List<Map<String, Object>> getCategoryList() {
        String url = apiBaseUrl + "/api/category/list";
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                return (List<Map<String, Object>>) responseBody.get("data");
            }
            
            log.error("获取分类列表失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用分类列表API失败", e);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 获取标签列表
     * 
     * @param limit 限制数量
     * @return 标签列表
     */
    public List<Map<String, Object>> getTagList(int limit) {
        String url = apiBaseUrl + "/api/tag/list?limit=" + limit;
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                return (List<Map<String, Object>>) responseBody.get("data");
            }
            
            log.error("获取标签列表失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用标签列表API失败", e);
        }
        
        return Collections.emptyList();
    }
    
    /**
     * 搜索新闻
     * 
     * @param keyword 关键词
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    public Map<String, Object> searchNews(String keyword, int pageNum, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiBaseUrl + "/api/news/search")
                .queryParam("keyword", keyword)
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize);
                
        String url = builder.toUriString();
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                return (Map<String, Object>) responseBody.get("data");
            }
            
            log.error("搜索新闻失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用搜索API失败: {}", keyword, e);
        }
        
        return new HashMap<>() {{
            put("list", Collections.emptyList());
            put("total", 0);
            put("pages", 0);
        }};
    }
} 