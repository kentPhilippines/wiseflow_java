package com.wiseflow.controller;

import com.wiseflow.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
public class CrawlerController {
    private final CrawlerService crawlerService;
    
    @PostMapping("/start")
    public String startCrawl() {
        try {
            crawlerService.scheduledCrawl();
            return "Crawl started successfully";
        } catch (Exception e) {
            return "Failed to start crawl: " + e.getMessage();
        }
    }
} 