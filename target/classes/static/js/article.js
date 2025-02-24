let currentPage = 0;
let currentCategory = null;

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', () => {
    loadArticles();
});

// 加载文章列表
async function loadArticles(page = 0, categoryId = null) {
    try {
        let url = `/api/articles?page=${page}&size=20&sort=createdAt,desc`;
        if (categoryId) {
            url += `&categoryId=${categoryId}`;
        }
        
        const response = await fetch(url);
        const data = await response.json();
        
        if (response.ok) {
            renderCategories(data.categories, data.currentCategoryId);
            renderArticles(data.articles);
            renderPagination(data.articles);
            currentPage = page;
            currentCategory = categoryId;
        } else {
            showAlert(data.error || '加载文章失败', 'danger');
        }
    } catch (error) {
        showAlert('加载文章失败: ' + error.message, 'danger');
    }
}

// 渲染分类导航
function renderCategories(categories, currentCategoryId) {
    const nav = document.getElementById('categoryNav');
    nav.innerHTML = `
        <a class="nav-link ${!currentCategoryId ? 'active' : ''}" 
           href="javascript:void(0)" onclick="loadArticles(0, null)">全部</a>
        ${categories.map(category => `
            <a class="nav-link ${category.id === currentCategoryId ? 'active' : ''}"
               href="javascript:void(0)" 
               onclick="loadArticles(0, ${category.id})">${category.name}</a>
        `).join('')}
    `;
}

// 渲染文章列表
function renderArticles(pageData) {
    const container = document.getElementById('articleList');
    
    if (pageData.empty) {
        container.innerHTML = '<div class="col-12"><div class="alert alert-info">暂无文章数据</div></div>';
        return;
    }
    
    container.innerHTML = pageData.content.map(article => `
        <div class="col-md-12">
            <div class="card mb-3">
                <div class="card-body">
                    <h5 class="card-title">
                        <a href="/article.html?id=${article.id}" class="text-decoration-none">
                            ${article.title}
                        </a>
                    </h5>
                    <p class="card-text">${article.summary || '暂无摘要'}</p>
                    <div class="d-flex justify-content-between">
                        <small class="text-muted">
                            <span class="badge bg-primary">${article.categoryName}</span>
                            ${article.author || '未知作者'}
                        </small>
                        <small class="text-muted">
                            ${new Date(article.createdAt).toLocaleString()}
                        </small>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

// 渲染分页
function renderPagination(pageData) {
    const nav = document.getElementById('pagination');
    if (pageData.totalPages <= 1) {
        nav.innerHTML = '';
        return;
    }
    
    nav.innerHTML = `
        <ul class="pagination justify-content-center">
            <li class="page-item ${pageData.first ? 'disabled' : ''}">
                <a class="page-link" href="javascript:void(0)" 
                   onclick="loadArticles(${pageData.number - 1}, ${currentCategory})">上一页</a>
            </li>
            ${Array.from({length: pageData.totalPages}, (_, i) => `
                <li class="page-item ${i === pageData.number ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0)"
                       onclick="loadArticles(${i}, ${currentCategory})">${i + 1}</a>
                </li>
            `).join('')}
            <li class="page-item ${pageData.last ? 'disabled' : ''}">
                <a class="page-link" href="javascript:void(0)"
                   onclick="loadArticles(${pageData.number + 1}, ${currentCategory})">下一页</a>
            </li>
        </ul>
    `;
}

// 搜索文章
async function searchArticles() {
    const keyword = document.getElementById('searchInput').value.trim();
    if (!keyword) {
        loadArticles();
        return;
    }
    
    try {
        const response = await fetch(`/api/articles/search?keyword=${encodeURIComponent(keyword)}`);
        const data = await response.json();
        
        if (response.ok) {
            renderArticles(data);
            renderPagination(data);
        } else {
            showAlert('搜索失败: ' + data.error, 'danger');
        }
    } catch (error) {
        showAlert('搜索失败: ' + error.message, 'danger');
    }
}

// 清理缓存
async function clearCache() {
    if (!confirm('确定要清理缓存吗？')) {
        return;
    }
    
    try {
        const response = await fetch('/api/articles/cache/clear', {
            method: 'POST'
        });
        const data = await response.json();
        
        showAlert(data.message, data.success ? 'success' : 'danger');
    } catch (error) {
        showAlert('��理缓存失败: ' + error.message, 'danger');
    }
}

// 清理过期文章
async function clearExpired() {
    if (!confirm('确定要清理过期文章吗？此操作不可恢复！')) {
        return;
    }
    
    try {
        const response = await fetch('/api/articles/expired/clear', {
            method: 'POST'
        });
        const data = await response.json();
        
        showAlert(data.message, data.success ? 'success' : 'danger');
        if (data.success) {
            setTimeout(() => loadArticles(), 1500);
        }
    } catch (error) {
        showAlert('清理过期文章失败: ' + error.message, 'danger');
    }
}

// 显示提示框
function showAlert(message, type) {
    const alertBox = document.getElementById('alertBox');
    const alertMessage = document.getElementById('alertMessage');
    alertBox.style.display = 'block';
    alertBox.className = `alert alert-${type} alert-dismissible fade show`;
    alertMessage.textContent = message;
}

// 关闭提示框
function closeAlert() {
    const alertBox = document.getElementById('alertBox');
    alertBox.style.display = 'none';
} 