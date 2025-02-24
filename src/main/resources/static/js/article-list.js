function renderArticleList(articles) {
    const articleList = document.getElementById('articleList');
    articleList.innerHTML = articles.map(article => `
        <tr>
            <td>
                <a href="/article.html?id=${article.id}">${article.title}</a>
            </td>
            <td>${article.categoryName}</td>
            <td>${article.author || '未知作者'}</td>
            <td>${new Date(article.createdAt).toLocaleString()}</td>
            <td>
                <span class="badge ${article.synced ? 'bg-success' : 'bg-warning'}">
                    ${article.synced ? '已同步' : '未同步'}
                    ${article.syncTime ? `<br>${new Date(article.syncTime).toLocaleString()}` : ''}
                </span>
            </td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="syncArticle(${article.id})">
                    ${article.synced ? '重新同步' : '同步'}
                </button>
            </td>
        </tr>
    `).join('');
}

// 添加手动同步功能
async function syncArticle(id) {
    try {
        const response = await fetch(`/api/articles/${id}/sync`, {
            method: 'POST'
        });
        
        if (response.ok) {
            alert('同步请求已发送');
            loadArticles(); // 重新加载列表
        } else {
            alert('同步请求失败');
        }
    } catch (error) {
        alert('同步请求失败: ' + error.message);
    }
} 