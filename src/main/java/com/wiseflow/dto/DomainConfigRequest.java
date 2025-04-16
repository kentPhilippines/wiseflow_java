package com.wiseflow.dto;

import lombok.Data;
import java.util.List;

@Data
public class DomainConfigRequest {
    
    /**
     * 域名
     */
    private String domain;
    
    /**
     * 配置ID列表
     */
    private List<Long> configIds;
} 