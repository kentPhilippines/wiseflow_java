package com.wiseflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 索引页面控制器
 */
@Controller
@RequestMapping("/index")
public class Index {
    /**
     * 处理/index路径请求
     * @return 返回domain视图
     */
    @GetMapping
    public String index() {
        return "domain";
    }
}
