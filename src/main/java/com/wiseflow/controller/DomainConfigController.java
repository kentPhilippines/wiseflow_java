package com.wiseflow.controller;

import com.wiseflow.dto.DomainConfigRequest;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/domain")
public class DomainConfigController {

    @Autowired
    private DomainConfigService domainConfigService;

    /**
     * 保存域名SEO关键词配置
     */
    @PostMapping("/{id}/config/keyword")
    public Result saveKeywordConfig(@PathVariable Long id, @RequestBody DomainConfigRequest request) {
        domainConfigService.saveKeywordConfig(id, request);
        return Result.success();
    }

    /**
     * 保存域名评论规则配置
     */
    @PostMapping("/{id}/config/comment")
    public Result saveCommentConfig(@PathVariable Long id, @RequestBody DomainConfigRequest request) {
        domainConfigService.saveCommentConfig(id, request);
        return Result.success();
    }

    /**
     * 保存域名文章配置
     */
    @PostMapping("/{id}/config/article")
    public Result saveArticleConfig(@PathVariable Long id, @RequestBody DomainConfigRequest request) {
        domainConfigService.saveArticleConfig(id, request);
        return Result.success();
    }




    /**
     * 删除配置 逻辑
     * 1， 获取到这额批次的所有域名
     * 2，根据域名 查询域名配置【关键词，评论，文章】  并删除 配置内容
     * 3，删除域名配置
     * 4，删除文章和域名的绑定关系
     * 5，删除域名
     */

    /**
     * 批量删除指定手机号下的域名配置
     */
    @PostMapping("/batch-delete-by-phone")
    public Result batchDeleteByPhone(@RequestBody BatchDeleteRequest request) {
        try {
            DomainConfigService.BatchDeleteResult result = domainConfigService.batchDeleteByPhone(
                request.getPhone(), 
                request.getDomainIds()
            );
            
            if (result.isSuccess()) {
                return Result.success(result);
            } else {
                return Result.error(result.getMessage());
            }
        } catch (Exception e) {
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除请求参数
     */
    public static class BatchDeleteRequest {
        private String phone;
        private List<Integer> domainIds;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public List<Integer> getDomainIds() {
            return domainIds;
        }

        public void setDomainIds(List<Integer> domainIds) {
            this.domainIds = domainIds;
        }
    }

} 