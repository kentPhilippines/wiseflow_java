package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wiseflow.entity.Activity;
import com.wiseflow.mapper.ActivityMapper;
import com.wiseflow.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public void recordActivity(String type, String description, String operator) {
        Activity activity = new Activity();
        activity.setType(type);
        activity.setDescription(description);
        activity.setOperator(operator);
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        
        activityMapper.insert(activity);
    }

    @Override
    public List<Activity> getRecentActivities(int limit) {
        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Activity::getCreateTime)
                   .last("LIMIT " + limit);
        
        return activityMapper.selectList(queryWrapper);
    }
} 