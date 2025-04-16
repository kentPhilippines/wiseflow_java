package com.wiseflow.controller;

import com.wiseflow.common.Result;
import com.wiseflow.service.DomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/domains")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;

    @GetMapping
    public Result<List<Map<String, Object>>> getDomains() {
        List<Map<String, Object>> domains = domainService.getDomainList();
        return Result.success(domains);
    }
} 