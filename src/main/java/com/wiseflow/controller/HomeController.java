package com.wiseflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 首页控制器，处理根路径请求
 */
@Controller
@RequestMapping("/")
public class HomeController {
    
    /**
     * 处理根路径请求，重定向到/index
     * @return 重定向到index页面
     */
    @GetMapping
    public String home() {
        return "redirect:/index";
    }
} 