/**
 * 漏洞知识搜索和分页状态
 */
let knowledgeCurrentPage = 1;

const knowledgePageSize = 6;

let knowledgeTotalPages = 1;

let knowledgeTotal = 0;

let knowledgeCategoriesLoaded = false;



/**
 * HTML 转义，避免接口数据被当成 HTML 执行
 */
function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * 请求平台 API
 */
async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options
    });

    const result = await response.json();

    if (response.status === 401) {
        window.location.href = "/login";
        throw new Error("登录状态已失效");
    }

    if (!response.ok || !result.success) {
        throw new Error(result.message || "请求失败");
    }

    return result.data;
}

/**
 * 加载用户信息
 */
async function loadUser() {
    const user = await apiRequest("/api/user/me");

    const nickname = user.nickname || user.username || "同学";
    const avatar = nickname.substring(0, 1);

    document.getElementById("welcomeName").textContent = nickname;
    document.getElementById("sidebarName").textContent = nickname;
    document.getElementById("topbarName").textContent = nickname;
    document.getElementById("sidebarAvatar").textContent = avatar;
    document.getElementById("topbarAvatar").textContent = avatar;
}

/**
 * 加载实验资源
 */
async function loadExperiments() {
    const container = document.getElementById("experimentList");

    try {
        const data = await apiRequest("/api/experiments?pageNum=1&pageSize=4");

        document.getElementById("experimentCount").textContent =
            data.total ?? 0;

        const records = data.records || [];

        if (records.length === 0) {
            container.innerHTML = '<div class="empty">暂时没有可用的实验资源</div>';
            return;
        }

        container.innerHTML = records.map(item => `
            <a
                class="resource-card resource-link"
                href="/student/experiments/${encodeURIComponent(item.id)}"
                aria-label="查看实验：${escapeHtml(item.name)}"
            >
                <h3>${escapeHtml(item.name)}</h3>

                <p>${escapeHtml(item.description || "暂无实验介绍")}</p>

                <div class="resource-meta">
                    <span class="difficulty">
                        ${escapeHtml(item.difficulty || "未设置难度")}
                    </span>

                    <span>实验编号：${escapeHtml(item.id)}</span>
                </div>

                <div class="resource-enter">查看实验详情 →</div>
            </a>
        `).join("");

    } catch (error) {
        container.innerHTML =
            `<div class="empty">${escapeHtml(error.message)}</div>`;
    }
}


/**
 * 加载漏洞分类
 */
async function loadKnowledgeCategories() {
    const select = document.getElementById("knowledgeCategory");

    if (!select) {
        return;
    }

    try {
        const categories = await apiRequest("/api/categories");

        // 清空原有选项，保留全部分类
        select.innerHTML = '<option value="">全部分类</option>';

        if (!Array.isArray(categories)) {
            throw new Error("分类数据格式错误");
        }

        categories.forEach(category => {
            const option = document.createElement("option");

            option.value = String(category.id);
            option.textContent = category.name || "未命名分类";

            select.appendChild(option);
        });

        knowledgeCategoriesLoaded = true;

    } catch (error) {
        console.error("加载漏洞分类失败：", error);

        showKnowledgeError(
            "漏洞分类加载失败：" + error.message
        );
    }
}



/**
 * 加载漏洞知识
 *
 * 支持：
 * 1. 关键词搜索
 * 2. 分类筛选
 * 3. 关键词和分类联合筛选
 * 4. 分页
 * 5. 保留原有知识详情跳转
 */
async function loadVulnerabilities() {

    const container = document.getElementById("vulnerabilityList");

    const resultText = document.getElementById("knowledgeResultText");

    const pageInfo = document.getElementById("knowledgePageInfo");

    const prevButton = document.getElementById("knowledgePrevButton");

    const nextButton = document.getElementById("knowledgeNextButton");

    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="loading">正在加载漏洞知识...</div>
    `;

    resultText.textContent = "正在查询...";

    prevButton.disabled = true;
    nextButton.disabled = true;

    try {

        const keyword = document
            .getElementById("knowledgeKeyword")
            .value
            .trim();

        const categoryId = document
            .getElementById("knowledgeCategory")
            .value;

        // 构建查询参数
        const params = new URLSearchParams();

        params.set("pageNum", String(knowledgeCurrentPage));

        params.set("pageSize", String(knowledgePageSize));

        if (keyword) {
            params.set("keyword", keyword);
        }

        if (categoryId) {
            params.set("categoryId", categoryId);
        }

        // 调用已有后端分页查询接口
        const data = await apiRequest(
            `/api/vulnerabilities?${params.toString()}`
        );

        const records = Array.isArray(data.records)
            ? data.records
            : [];

        knowledgeTotal = Number(data.total || 0);

        knowledgeTotalPages = Math.max(
            1,
            Number(data.totalPages || 1)
        );

        knowledgeCurrentPage = Number(
            data.pageNum || knowledgeCurrentPage
        );

        // 首次或无筛选条件时，同步更新首页知识总数
        if (!keyword && !categoryId) {
            document.getElementById("vulnerabilityCount").textContent =
                knowledgeTotal;
        }

        // 更新搜索结果
        resultText.textContent =
            `共找到 ${knowledgeTotal} 条漏洞知识`;

        // 更新分页信息
        pageInfo.textContent =
            `第 ${knowledgeCurrentPage} 页 / 共 ${knowledgeTotalPages} 页`;

        prevButton.disabled = knowledgeCurrentPage <= 1;

        nextButton.disabled =
            knowledgeCurrentPage >= knowledgeTotalPages;

        // 没有数据
        if (records.length === 0) {

            container.innerHTML = `
                <div class="knowledge-empty">
                    <div class="knowledge-empty-icon">⌕</div>
                    <h3>没有找到相关漏洞知识</h3>
                    <p>请尝试更换关键词或选择其他分类</p>
                </div>
            `;

            return;
        }

        // 渲染漏洞知识卡片
        container.innerHTML = records.map(item => `
            <a
                class="knowledge-card knowledge-link"
                href="/student/knowledge/${encodeURIComponent(item.id)}"
                aria-label="查看漏洞知识：${escapeHtml(item.title)}"
            >
                <h3>${escapeHtml(item.title)}</h3>

                <p>
                    ${escapeHtml(item.summary || "暂无知识简介")}
                </p>

                <div class="knowledge-enter">
                    查看知识详情 →
                </div>
            </a>
        `).join("");

    } catch (error) {

        console.error("加载漏洞知识失败：", error);

        container.innerHTML = `
            <div class="knowledge-empty">
                <h3>漏洞知识加载失败</h3>
                <p>${escapeHtml(error.message)}</p>
            </div>
        `;

        resultText.textContent = "查询失败";

        pageInfo.textContent = "暂不可用";

        showKnowledgeError(error.message);
    }
}



/**
 * 显示漏洞知识错误
 */
function showKnowledgeError(message) {
    const resultText = document.getElementById("knowledgeResultText");

    if (resultText) {
        resultText.textContent = message;
    }
}

/**
 * 执行漏洞知识搜索
 */
function searchKnowledge() {

    knowledgeCurrentPage = 1;

    loadVulnerabilities();
}

/**
 * 重置漏洞知识搜索条件
 */
function resetKnowledgeFilters() {

    document.getElementById("knowledgeKeyword").value = "";

    document.getElementById("knowledgeCategory").value = "";

    knowledgeCurrentPage = 1;

    loadVulnerabilities();
}

/**
 * 初始化漏洞知识搜索功能
 */
function initializeKnowledgeSearch() {

    const keywordInput = document.getElementById("knowledgeKeyword");

    const categorySelect = document.getElementById("knowledgeCategory");

    const searchButton = document.getElementById("knowledgeSearchButton");

    const resetButton = document.getElementById("knowledgeResetButton");

    const prevButton = document.getElementById("knowledgePrevButton");

    const nextButton = document.getElementById("knowledgeNextButton");

    if (!keywordInput || !categorySelect) {
        return;
    }

    // 点击搜索按钮
    searchButton.addEventListener("click", searchKnowledge);

    // 按 Enter 搜索
    keywordInput.addEventListener("keydown", event => {

        if (event.key === "Enter") {
            searchKnowledge();
        }

    });

    // 切换分类立即筛选
    categorySelect.addEventListener("change", () => {

        knowledgeCurrentPage = 1;

        loadVulnerabilities();

    });

    // 清空筛选条件
    resetButton.addEventListener("click", resetKnowledgeFilters);

    // 上一页
    prevButton.addEventListener("click", () => {

        if (knowledgeCurrentPage > 1) {

            knowledgeCurrentPage--;

            loadVulnerabilities();
        }

    });

    // 下一页
    nextButton.addEventListener("click", () => {

        if (knowledgeCurrentPage < knowledgeTotalPages) {

            knowledgeCurrentPage++;

            loadVulnerabilities();
        }

    });
}

/**
 * 加载我的任务
 */
async function loadTasks() {
    const container = document.getElementById("taskList");

    try {
        const data = await apiRequest("/api/tasks?pageNum=1&pageSize=10");

        const records = data.records || [];

        document.getElementById("taskCount").textContent =
            data.total ?? records.length;

        const completed = records.filter(item =>
            item.status === "COMPLETED"
        ).length;

        document.getElementById("completedCount").textContent = completed;

        if (records.length === 0) {
            container.innerHTML = `
                <tr>
                    <td colspan="4" class="empty">你还没有领取实验任务</td>
                </tr>
            `;
            return;
        }

        const statusNames = {
            CLAIMED: "已领取",
            IN_PROGRESS: "进行中",
            SUBMITTED: "待审核",
            COMPLETED: "已完成"
        };

        container.innerHTML = records.map(item => `
            <tr>
                <td>${escapeHtml(item.id)}</td>
                <td>${escapeHtml(item.experimentId)}</td>
                <td>
                    <span class="status ${escapeHtml(item.status)}">
                        ${escapeHtml(statusNames[item.status] || item.status)}
                    </span>
                </td>
                <td>${escapeHtml(item.createdAt || "-")}</td>
            </tr>
        `).join("");

    } catch (error) {
        container.innerHTML = `
            <tr>
                <td colspan="4" class="empty">${escapeHtml(error.message)}</td>
            </tr>
        `;
    }
}

/**
 * 退出登录
 */
document.getElementById("logoutButton").addEventListener("click", async function () {
    if (!confirm("确定退出当前账号吗？")) {
        return;
    }

    try {
        await fetch("/api/user/logout", {
            method: "POST",
            credentials: "same-origin"
        });
    } finally {
        window.location.href = "/login";
    }
});

/**
 * 页面初始化
 */
async function initializeDashboard() {

    try {

        await loadUser();

        // 初始化漏洞知识搜索事件
        initializeKnowledgeSearch();

        // 并行加载首页数据
        await Promise.all([

            loadExperiments(),

            loadKnowledgeCategories(),

            loadTasks()

        ]);

        // 分类加载完成后再加载漏洞知识
        await loadVulnerabilities();

    } catch (error) {

        console.error("工作台初始化失败：", error);

    }
}

initializeDashboard();