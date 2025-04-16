// 存储选中的关键词ID
let selectedKeywordIds = [];

// 显示关键词管理模态框
function showKeywordsModal(domainConfigId, domainName) {
    // 如果模态框不存在则创建
    if ($('#keywordsModal').length === 0) {
        const modalHtml = `
        <div class="modal fade" id="keywordsModal" tabindex="-1" aria-labelledby="keywordsModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-xl">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="keywordsModalLabel">SEO关键词管理 - ${domainName}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                    </div>
                    <div class="modal-body">
                        <input type="hidden" id="keywordDomainConfigId" value="${domainConfigId}">
                        
                        <!-- 统计卡片 -->
                        <div class="row mb-3">
                            <div class="col-md-3">
                                <div class="card text-white bg-primary">
                                    <div class="card-body">
                                        <h5 class="card-title">总关键词数</h5>
                                        <p class="card-text" id="totalKeywordsCount">0</p>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="card text-white bg-success">
                                    <div class="card-body">
                                        <h5 class="card-title">主关键词</h5>
                                        <p class="card-text" id="mainKeywordsCount">0</p>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="card text-white bg-info">
                                    <div class="card-body">
                                        <h5 class="card-title">长尾关键词</h5>
                                        <p class="card-text" id="longTailKeywordsCount">0</p>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-3">
                                <div class="card text-white bg-warning">
                                    <div class="card-body">
                                        <h5 class="card-title">首页展示</h5>
                                        <p class="card-text" id="homepageKeywordsCount">0</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <!-- 批量操作按钮 -->
                        <div class="mb-3">
                            <button class="btn btn-success me-2" onclick="batchEnableKeywords()">
                                <i class="bi bi-check-circle"></i> 批量启用
                            </button>
                            <button class="btn btn-danger me-2" onclick="batchDisableKeywords()">
                                <i class="bi bi-x-circle"></i> 批量禁用
                            </button>
                            <button class="btn btn-danger me-2" onclick="batchDeleteKeywords()">
                                <i class="bi bi-trash"></i> 批量删除
                            </button>
                            <button class="btn btn-primary me-2" onclick="exportKeywords()">
                                <i class="bi bi-download"></i> 导出关键词
                            </button>
                            <button class="btn btn-success" onclick="showAddKeywordModal(${domainConfigId})">
                                <i class="bi bi-plus-circle"></i> 添加关键词
                            </button>
                        </div>
                        
                        <!-- 关键词表格 -->
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                    <tr>
                                        <th>
                                            <input type="checkbox" id="selectAllKeywords" onchange="toggleAllKeywords(this)">
                                        </th>
                                        <th>ID</th>
                                        <th>关键词</th>
                                        <th>类型</th>
                                        <th>权重</th>
                                        <th>使用场景</th>
                                        <th>情感倾向</th>
                                        <th>最大插入次数</th>
                                        <th>首页展示</th>
                                        <th>状态</th>
                                        <th>操作</th>
                                    </tr>
                                </thead>
                                <tbody id="keywordsTableBody">
                                    <!-- 这里将动态插入关键词数据 -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 关键词编辑模态框 -->
        <div class="modal fade" id="editKeywordModal" tabindex="-1" aria-labelledby="editKeywordModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="editKeywordModalLabel">编辑关键词</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                    </div>
                    <div class="modal-body">
                        <form id="keywordForm">
                            <input type="hidden" id="keywordId">
                            <input type="hidden" id="keywordConfigId">
                            
                            <div class="mb-3">
                                <label for="keywordName" class="form-label">关键词</label>
                                <input type="text" class="form-control" id="keywordName" required>
                            </div>
                            
                            <div class="mb-3">
                                <label for="keywordType" class="form-label">类型</label>
                                <select class="form-select" id="keywordType">
                                    <option value="1">主关键词</option>
                                    <option value="2">长尾关键词</option>
                                </select>
                            </div>
                            
                            <div class="mb-3">
                                <label for="keywordWeight" class="form-label">权重</label>
                                <input type="number" class="form-control" id="keywordWeight" min="1" max="100" value="10">
                            </div>
                            
                            <div class="mb-3">
                                <label for="keywordUseScene" class="form-label">使用场景</label>
                                <select class="form-select" id="keywordUseScene">
                                    <option value="1">标题</option>
                                    <option value="2">内容</option>
                                    <option value="3">标题和内容</option>
                                </select>
                            </div>
                            
                            <div class="mb-3">
                                <label for="keywordSentiment" class="form-label">情感倾向</label>
                                <select class="form-select" id="keywordSentiment">
                                    <option value="1">正面</option>
                                    <option value="2">中性</option>
                                    <option value="3">负面</option>
                                </select>
                            </div>
                            
                            <div class="mb-3">
                                <label for="keywordMaxInsertions" class="form-label">最大插入次数</label>
                                <input type="number" class="form-control" id="keywordMaxInsertions" min="1" max="10" value="3">
                            </div>
                            
                            <div class="form-check mb-3">
                                <input class="form-check-input" type="checkbox" id="keywordShowOnHomepage">
                                <label class="form-check-label" for="keywordShowOnHomepage">
                                    首页展示
                                </label>
                            </div>
                            
                            <div class="form-check mb-3">
                                <input class="form-check-input" type="checkbox" id="keywordEnabled" checked>
                                <label class="form-check-label" for="keywordEnabled">
                                    启用
                                </label>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary" onclick="saveKeyword()">保存</button>
                    </div>
                </div>
            </div>
        </div>
        `;
        
        $('body').append(modalHtml);
    }
    
    // 设置域名配置ID并显示模态框
    $('#keywordDomainConfigId').val(domainConfigId);
    $('#keywordsModalLabel').text(`SEO关键词管理 - ${domainName}`);
    
    // 清空选中的关键词IDs
    selectedKeywordIds = [];
    
    // 加载关键词数据
    loadKeywords(domainConfigId);
    
    // 显示模态框
    const keywordsModal = new bootstrap.Modal(document.getElementById('keywordsModal'));
    keywordsModal.show();
}

// 加载关键词数据
function loadKeywords(domainConfigId) {
    fetch(`/api/seo/keywords/list/${domainConfigId}`)
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                renderKeywordTable(result.data);
                updateKeywordStats(result.data);
            } else {
                alert('加载关键词失败：' + result.message);
            }
        })
        .catch(error => {
            console.error('加载关键词出错：', error);
            alert('加载关键词出错，请查看控制台日志');
        });
}

// 渲染关键词表格
function renderKeywordTable(keywords) {
    const tableBody = $('#keywordsTableBody');
    tableBody.empty();
    
    keywords.forEach(keyword => {
        const typeText = keyword.type === 1 ? '主关键词' : '长尾关键词';
        const useSceneText = getUseSceneText(keyword.useScene);
        const sentimentText = getSentimentText(keyword.sentiment);
        const statusBadge = keyword.enabled ? 
            '<span class="badge bg-success">启用</span>' : 
            '<span class="badge bg-danger">禁用</span>';
        const homepageDisplay = keyword.showOnHomepage ? 
            '<span class="badge bg-info">是</span>' : 
            '<span class="badge bg-secondary">否</span>';
        
        const row = `
        <tr>
            <td><input type="checkbox" class="keyword-checkbox" value="${keyword.id}"></td>
            <td>${keyword.id}</td>
            <td>${keyword.keyword}</td>
            <td>${typeText}</td>
            <td>${keyword.weight}</td>
            <td>${useSceneText}</td>
            <td>${sentimentText}</td>
            <td>${keyword.maxInsertions}</td>
            <td>${homepageDisplay}</td>
            <td>${statusBadge}</td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="editKeyword(${keyword.id})">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="deleteKeyword(${keyword.id})">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        </tr>
        `;
        
        tableBody.append(row);
    });
    
    // 绑定复选框事件
    $('.keyword-checkbox').on('change', function() {
        const keywordId = parseInt($(this).val());
        if ($(this).is(':checked')) {
            if (!selectedKeywordIds.includes(keywordId)) {
                selectedKeywordIds.push(keywordId);
            }
        } else {
            const index = selectedKeywordIds.indexOf(keywordId);
            if (index > -1) {
                selectedKeywordIds.splice(index, 1);
            }
        }
        
        // 更新全选复选框状态
        updateSelectAllCheckbox();
    });
}

// 更新关键词统计信息
function updateKeywordStats(keywords) {
    const totalCount = keywords.length;
    const mainCount = keywords.filter(k => k.type === 1).length;
    const longTailCount = keywords.filter(k => k.type === 2).length;
    const homepageCount = keywords.filter(k => k.showOnHomepage).length;
    
    $('#totalKeywordsCount').text(totalCount);
    $('#mainKeywordsCount').text(mainCount);
    $('#longTailKeywordsCount').text(longTailCount);
    $('#homepageKeywordsCount').text(homepageCount);
}

// 获取使用场景文本
function getUseSceneText(useScene) {
    switch(useScene) {
        case 1: return '标题';
        case 2: return '内容';
        case 3: return '标题和内容';
        default: return '未知';
    }
}

// 获取情感倾向文本
function getSentimentText(sentiment) {
    switch(sentiment) {
        case 1: return '正面';
        case 2: return '中性';
        case 3: return '负面';
        default: return '未知';
    }
}

// 更新全选复选框状态
function updateSelectAllCheckbox() {
    const totalCheckboxes = $('.keyword-checkbox').length;
    const checkedCheckboxes = $('.keyword-checkbox:checked').length;
    
    if (checkedCheckboxes === 0) {
        $('#selectAllKeywords').prop('checked', false);
        $('#selectAllKeywords').prop('indeterminate', false);
    } else if (checkedCheckboxes === totalCheckboxes) {
        $('#selectAllKeywords').prop('checked', true);
        $('#selectAllKeywords').prop('indeterminate', false);
    } else {
        $('#selectAllKeywords').prop('indeterminate', true);
    }
}

// 显示添加关键词模态框
function showAddKeywordModal(domainConfigId) {
    // 重置表单
    $('#keywordForm')[0].reset();
    $('#keywordId').val('');
    $('#keywordConfigId').val(domainConfigId);
    
    // 设置默认值
    $('#keywordType').val('1');
    $('#keywordWeight').val('10');
    $('#keywordUseScene').val('3');
    $('#keywordSentiment').val('2');
    $('#keywordMaxInsertions').val('3');
    $('#keywordEnabled').prop('checked', true);
    
    // 显示模态框
    $('#editKeywordModalLabel').text('添加关键词');
    const editModal = new bootstrap.Modal(document.getElementById('editKeywordModal'));
    editModal.show();
}

// 编辑关键词
function editKeyword(id) {
    fetch(`/api/seo/keywords/${id}`)
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                const keyword = result.data;
                
                // 填充表单
                $('#keywordId').val(keyword.id);
                $('#keywordConfigId').val(keyword.domainConfigId);
                $('#keywordName').val(keyword.keyword);
                $('#keywordType').val(keyword.type);
                $('#keywordWeight').val(keyword.weight);
                $('#keywordUseScene').val(keyword.useScene);
                $('#keywordSentiment').val(keyword.sentiment);
                $('#keywordMaxInsertions').val(keyword.maxInsertions);
                $('#keywordShowOnHomepage').prop('checked', keyword.showOnHomepage);
                $('#keywordEnabled').prop('checked', keyword.enabled);
                
                // 显示模态框
                $('#editKeywordModalLabel').text('编辑关键词');
                const editModal = new bootstrap.Modal(document.getElementById('editKeywordModal'));
                editModal.show();
            } else {
                alert('获取关键词信息失败：' + result.message);
            }
        })
        .catch(error => {
            console.error('获取关键词信息出错：', error);
            alert('获取关键词信息出错，请查看控制台日志');
        });
}

// 保存关键词
function saveKeyword() {
    const id = $('#keywordId').val();
    const keywordData = {
        domainConfig: $('#keywordConfigId').val(),
        keyword: $('#keywordName').val().trim(),
        type: parseInt($('#keywordType').val()),
        weight: parseInt($('#keywordWeight').val()),
        useScene: parseInt($('#keywordUseScene').val()),
        sentiment: parseInt($('#keywordSentiment').val()),
        maxInsertions: parseInt($('#keywordMaxInsertions').val()),
        showOnHomepage: $('#keywordShowOnHomepage').is(':checked'),
        enabled: $('#keywordEnabled').is(':checked')
    };
    
    // 验证输入
    if (!keywordData.keyword) {
        alert('请输入关键词');
        return;
    }
    
    let url = '/api/seo/keywords';
    let method = 'POST';
    
    // 如果有ID，则为更新操作
    if (id) {
        url = `/api/seo/keywords/${id}`;
        method = 'PUT';
        keywordData.id = parseInt(id);
    }
    
    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(keywordData)
    })
    .then(response => response.json())
    .then(result => {
        if (result.code === 200) {
            // 关闭编辑模态框
            const editModal = bootstrap.Modal.getInstance(document.getElementById('editKeywordModal'));
            editModal.hide();
            
            // 重新加载关键词列表
            loadKeywords($('#keywordDomainConfigId').val());
            
            alert(id ? '关键词更新成功' : '关键词添加成功');
        } else {
            alert(id ? '更新关键词失败：' : '添加关键词失败：' + result.message);
        }
    })
    .catch(error => {
        console.error('保存关键词出错：', error);
        alert('保存关键词出错，请查看控制台日志');
    });
}

// 删除关键词
function deleteKeyword(id) {
    if (confirm('确定要删除这个关键词吗？此操作不可撤销。')) {
        fetch(`/api/seo/keywords/${id}`, {
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                // 重新加载关键词列表
                loadKeywords($('#keywordDomainConfigId').val());
                alert('关键词删除成功');
            } else {
                alert('删除关键词失败：' + result.message);
            }
        })
        .catch(error => {
            console.error('删除关键词出错：', error);
            alert('删除关键词出错，请查看控制台日志');
        });
    }
}

// 批量启用关键词
function batchEnableKeywords() {
    if (selectedKeywordIds.length === 0) {
        alert('请至少选择一个关键词');
        return;
    }
    
    if (confirm(`确定要启用选中的 ${selectedKeywordIds.length} 个关键词吗？`)) {
        const data = {
            ids: selectedKeywordIds,
            enabled: true
        };
        
        fetch('/api/seo/keywords/batch/status', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                // 重新加载关键词列表
                loadKeywords($('#keywordDomainConfigId').val());
                // 清空选中的关键词IDs
                selectedKeywordIds = [];
                alert('批量启用关键词成功');
            } else {
                alert('批量启用关键词失败：' + result.message);
            }
        })
        .catch(error => {
            console.error('批量启用关键词出错：', error);
            alert('批量启用关键词出错，请查看控制台日志');
        });
    }
}

// 批量禁用关键词
function batchDisableKeywords() {
    if (selectedKeywordIds.length === 0) {
        alert('请至少选择一个关键词');
        return;
    }
    
    if (confirm(`确定要禁用选中的 ${selectedKeywordIds.length} 个关键词吗？`)) {
        const data = {
            ids: selectedKeywordIds,
            enabled: false
        };
        
        fetch('/api/seo/keywords/batch/status', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                // 重新加载关键词列表
                loadKeywords($('#keywordDomainConfigId').val());
                // 清空选中的关键词IDs
                selectedKeywordIds = [];
                alert('批量禁用关键词成功');
            } else {
                alert('批量禁用关键词失败：' + result.message);
            }
        })
        .catch(error => {
            console.error('批量禁用关键词出错：', error);
            alert('批量禁用关键词出错，请查看控制台日志');
        });
    }
}

// 批量删除关键词
function batchDeleteKeywords() {
    if (selectedKeywordIds.length === 0) {
        alert('请至少选择一个关键词');
        return;
    }
    
    if (confirm(`确定要删除选中的 ${selectedKeywordIds.length} 个关键词吗？此操作不可撤销。`)) {
        fetch('/api/seo/keywords/batch', {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(selectedKeywordIds)
        })
        .then(response => response.json())
        .then(result => {
            if (result.code === 200) {
                // 重新加载关键词列表
                loadKeywords($('#keywordDomainConfigId').val());
                // 清空选中的关键词IDs
                selectedKeywordIds = [];
                alert('批量删除关键词成功');
            } else {
                alert('批量删除关键词失败：' + result.message);
            }
        })
        .catch(error => {
            console.error('批量删除关键词出错：', error);
            alert('批量删除关键词出错，请查看控制台日志');
        });
    }
}

// 导出关键词
function exportKeywords() {
    const domainConfigId = $('#keywordDomainConfigId').val();
    window.open(`/api/seo/keywords/export/${domainConfigId}`, '_blank');
}

// 全选/取消全选关键词
function toggleAllKeywords(checkbox) {
    const isChecked = $(checkbox).is(':checked');
    
    // 更新所有关键词复选框
    $('.keyword-checkbox').prop('checked', isChecked);
    
    // 更新选中的关键词IDs数组
    if (isChecked) {
        selectedKeywordIds = [];
        $('.keyword-checkbox').each(function() {
            selectedKeywordIds.push(parseInt($(this).val()));
        });
    } else {
        selectedKeywordIds = [];
    }
} 