package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.DomainSeoKeyword;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper 
public interface DomainSeoKeywordMapper extends BaseMapper<DomainSeoKeyword> {
    List<Long> selectKeywordIdsByDomain(String domain);
}