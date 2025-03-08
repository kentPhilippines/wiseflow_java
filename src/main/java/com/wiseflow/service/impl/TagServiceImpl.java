package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.Tag;
import com.wiseflow.mapper.TagMapper;
import com.wiseflow.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 标签服务实现类
 * 使用MyBatis-Plus实现，不使用事务管理
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    
    @Override
    public Tag save(Tag tag) {
        if (tag.getId() == null) {
            tagMapper.insert(tag);
        } else {
            tagMapper.updateById(tag);
        }
        return tag;
    }
    
    @Override
    public Optional<Tag> findById(Integer id) {
        return Optional.ofNullable(tagMapper.selectById(id));
    }
    
    @Override
    public Optional<Tag> findByName(String name) {
        return Optional.ofNullable(tagMapper.selectByName(name));
    }
    
    @Override
    public List<Tag> findAll() {
        return tagMapper.selectList(null);
    }
    
    @Override
    public Page<Tag> findAll(Page<Tag> pageable) {
        return tagMapper.selectPage(pageable, null);
    }
    
    @Override
    public void delete(Tag tag) {
        tagMapper.deleteById(tag.getId());
    }
    
    @Override
    public void deleteById(Integer id) {
        tagMapper.deleteById(id);
    }
    
    @Override
    public List<Tag> findHotTags(int limit) {
        return tagMapper.selectHotTags(limit);
    }
} 