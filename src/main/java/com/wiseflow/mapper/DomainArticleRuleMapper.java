package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.DomainArticleConfig;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DomainArticleRuleMapper extends BaseMapper<DomainArticleConfig> {
}