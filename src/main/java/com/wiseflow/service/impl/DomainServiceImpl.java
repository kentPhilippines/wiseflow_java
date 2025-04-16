package com.wiseflow.service.impl;

import com.wiseflow.service.DomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DomainServiceImpl implements DomainService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getDomainList() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    domain\n" +
                "FROM wf_domain_config\n" +
                "WHERE enabled = 1\n" +
                "ORDER BY sort_order";
        return jdbcTemplate.queryForList(sql);
    }
} 