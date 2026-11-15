
const labApiUrl = "/api/admin/labs";

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

async function requestLabList() {
    const response = await fetch(labApiUrl, {
        method: "GET",
        credentials: "same-origin"
    });

    if (response.status === 401 || response.status === 403) {
        window.location.href = "/login";
        throw new Error("登录状态已失效，请重新登录");
    }

    const result = await response.json();

    if (!response.ok || !result.success) {
        throw new Error(result.message || "获取靶场实例失败");
    }

    return Array.isArray(result.data) ? result.data : [];
}

function getStatusClass(status) {
    switch (status) {
        case "RUNNING":
            return "status-running";
        case "STOPPED":
        case "STOPPING":
            return "status-stopped";
        case "FAILED":
        case "EXPIRED":
            return "status-failed";
        default:
            return "status-other";
    }
}

function getStatusText(status) {
    const statusMap = {
        STARTING: "启动中",
        RUNNING: "运行中",
        STOPPING: "停止中",
        STOPPED: "已停止",
        FAILED: "启动失败",
        EXPIRED: "已过期"
    };

    return statusMap[status] || status || "未知";
}

function getContainerStatus(running) {
    return running
        ? '<span class="container-status container-up">运行中</span>'
        : '<span class="container-status container-down">未运行</span>';
}

function formatDate(value) {
    if (!value) {
        return "--";
    }

    return escapeHtml(String(value).replace("T", " "));
}

function renderLabRows(labs) {
    const tbody = document.getElementById("labTableBody");

    if (!labs.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="10" class="empty-cell">
                    暂无靶场实例记录
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = labs.map(lab => {
        const status = escapeHtml(lab.status);
        const statusClass = getStatusClass(lab.status);

        let accessCell = "--";

        if (lab.accessUrl) {
            const url = String(lab.accessUrl);

            // 只允许访问本机 DVWA 地址
            if (/^http:\/\/(localhost|127\.0\.0\.1):\d{1,5}$/.test(url)) {
                accessCell = `
                    <a class="access-link"
                       href="${escapeHtml(url)}"
                       target="_blank"
                       rel="noopener noreferrer">进入靶场</a>
                `;
            }
        }

        return `
            <tr>
                <td>${escapeHtml(lab.id)}</td>
                <td>${escapeHtml(lab.userId)}</td>
                <td>${escapeHtml(lab.taskId)}</td>
                <td>${escapeHtml(lab.instanceKey)}</td>
                <td>${escapeHtml(lab.hostPort ?? "--")}</td>
                <td>${getContainerStatus(lab.webRunning)}</td>
                <td>${getContainerStatus(lab.dbRunning)}</td>
                <td>
                    <span class="status-tag ${statusClass}">
                        ${escapeHtml(getStatusText(lab.status))}
                    </span>
                </td>
                <td>${formatDate(lab.startedAt)}</td>
                <td>${accessCell}</td>
            </tr>
        `;
    }).join("");
}

function updateStatistics(labs) {
    document.getElementById("totalCount").textContent = labs.length;

    document.getElementById("runningCount").textContent =
        labs.filter(item =>
            item.webRunning && item.dbRunning
        ).length;

    document.getElementById("failedCount").textContent =
        labs.filter(item =>
            item.status === "FAILED" ||
            item.status === "EXPIRED"
        ).length;

    document.getElementById("stoppedCount").textContent =
        labs.filter(item =>
            item.status === "STOPPED"
        ).length;
}

async function loadLabList() {
    const refreshButton = document.getElementById("refreshButton");
    const loadStatus = document.getElementById("loadStatus");
    const errorMessage = document.getElementById("errorMessage");

    refreshButton.disabled = true;
    loadStatus.textContent = "正在加载...";
    errorMessage.hidden = true;

    try {
        const labs = await requestLabList();

        renderLabRows(labs);
        updateStatistics(labs);

        loadStatus.textContent =
            `最近刷新：${new Date().toLocaleTimeString()}`;

    } catch (error) {
        console.error("加载靶场实例失败：", error);

        errorMessage.textContent =
            error.message || "加载靶场实例失败";

        errorMessage.hidden = false;
        loadStatus.textContent = "加载失败";

        document.getElementById("labTableBody").innerHTML = `
            <tr>
                <td colspan="10" class="empty-cell">
                    实例数据加载失败
                </td>
            </tr>
        `;

    } finally {
        refreshButton.disabled = false;
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("refreshButton")
        .addEventListener("click", loadLabList);

    loadLabList();
});
