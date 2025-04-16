package com.wiseflow.service;

import com.wiseflow.entity.Activity;
import java.util.List;

public interface ActivityService {
    
    /**
     * 记录新的活动
     *
     * @param type 活动类型
     * @param description 活动描述
     * @param operator 操作人
     */
    void recordActivity(String type, String description, String operator);
    
    /**
     * 获取最近的活动记录
     *
     * @param limit 限制返回的记录数
     * @return 活动记录列表
     */
    List<Activity> getRecentActivities(int limit);
} 