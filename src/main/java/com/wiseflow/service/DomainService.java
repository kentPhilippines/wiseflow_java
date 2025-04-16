package com.wiseflow.service;

import java.util.List;
import java.util.Map;

public interface DomainService {
    
    /**
     * 获取域名列表
     */
    List<Map<String, Object>> getDomainList();
} 