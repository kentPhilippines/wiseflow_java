package com.wiseflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用程序启动类
 * 启用异步处理和定时任务
 * 不使用事务管理
 * 启用缓存功能
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableCaching
@MapperScan("com.wiseflow.mapper")
public class WiseFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(WiseFlowApplication.class, args);
    }
}