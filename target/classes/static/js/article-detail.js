document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const id = urlParams.get('id');
    if (id) {
        loadArticleDetail(id);
    }
});

async function loadArticleDetail(id) {
    try {
        const response = await fetch(`/api/articles/${id}`);
        const article = await response.json();
        
        if (response.ok) {
            document.title = article.title;
            document.getElementById('articleDetail').innerHTML = `
                <h1 class="mb-4">${article.title}</h1>
                <div class="mb-3">
                    <span class="badge bg-primary">${article.categoryName}</span>
                    <small class="text-muted ms-2">${article.author || '未知作者'}</small>
                    <small class="text-muted ms-2">
                        ${new Date(article.createdAt).toLocaleString()}
                    </small>
                    <span class="badge ${article.synced ? 'bg-success' : 'bg-warning'} ms-2">
                        ${article.synced ? '已同步' : '未同步'}
                        ${article.syncTime ? new Date(article.syncTime).toLocaleString() : ''}
                    </span>
                </div>
                <div class="mb-4">
                    ${article.content.split('\n').map(p => `<p>${p}</p>`).join('')}
                </div>
                ${article.images && article.images.length > 0 ? `
                    <div class="mb-4">
                        <h5>相关图片：</h5>
                        <div class="row">
                            ${article.images.map(img => `
                                <div class="col-md-4 mb-3">
                                    <img src="${img}" class="img-fluid" alt="文章图片" referrerpolicy="no-referrer">
                                </div>
                            `).join('')}
                        </div>
                    </div>
                ` : ''}
            `;
        } else {
            document.getElementById('articleDetail').innerHTML = `
                <div class="alert alert-danger">
                    文章不存在或已被删除
                </div>
            `;
        }
    } catch (error) {
        document.getElementById('articleDetail').innerHTML = `
            <div class="alert alert-danger">
                加载文章失败: ${error.message}
            </div>
        `;
    }
} 