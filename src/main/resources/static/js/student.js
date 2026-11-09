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
            <div class="resource-card">
                <h3>${escapeHtml(item.name)}</h3>
                <p>${escapeHtml(item.description || "暂无实验介绍")}</p>
                <div class="resource-meta">
                    <span class="difficulty">${escapeHtml(item.difficulty || "未设置难度")}</span>
                    <span>实验编号：${escapeHtml(item.id)}</span>
                </div>
            </div>
        `).join("");

    } catch (error) {
        container.innerHTML =
            `<div class="empty">${escapeHtml(error.message)}</div>`;
    }
}

/**
 * 加载漏洞知识
 */
async function loadVulnerabilities() {
    const container = document.getElementById("vulnerabilityList");

    try {
        const data = await apiRequest(
            "/api/vulnerabilities?pageNum=1&pageSize=4"
        );

        document.getElementById("vulnerabilityCount").textContent =
            data.total ?? 0;

        const records = data.records || [];

        if (records.length === 0) {
            container.innerHTML = '<div class="empty">暂时没有可学习的漏洞知识</div>';
            return;
        }

        container.innerHTML = records.map(item => `
            <div class="knowledge-card">
                <h3>${escapeHtml(item.title)}</h3>
                <p>${escapeHtml(item.summary || "暂无知识简介")}</p>
            </div>
        `).join("");

    } catch (error) {
        container.innerHTML =
            `<div class="empty">${escapeHtml(error.message)}</div>`;
    }
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

        await Promise.all([
            loadExperiments(),
            loadVulnerabilities(),
            loadTasks()
        ]);

    } catch (error) {
        console.error("工作台初始化失败：", error);
    }
}

initializeDashboard();