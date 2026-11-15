
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

/**
 * 调用管理员靶场操作接口
 */
async function requestLabAction(id, action) {
    const response = await fetch(
        `${labApiUrl}/${encodeURIComponent(id)}/${action}`,
        {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Content-Type": "application/json"
            }
        }
    );

    if (response.status === 401 || response.status === 403) {
        window.location.href = "/login";
        throw new Error("登录状态已失效，请重新登录");
    }

    const result = await response.json();

    if (!response.ok || !result.success) {
        throw new Error(result.message || "实例操作失败");
    }

    return result;
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

/**
 * 根据实例状态生成可执行操作。
 */
function renderActions(lab) {
    const id = Number(lab.id);

    if (!Number.isSafeInteger(id) || id <= 0) {
        return '<span class="action-disabled">不可操作</span>';
    }

    const status = lab.status;

    if (status === "STARTING" || status === "STOPPING") {
        return '<span class="action-disabled">处理中</span>';
    }

    if (status === "RUNNING") {
        return `
            <div class="action-group">
                <button
                    class="action-button action-stop"
                    data-action="stop"
                    data-id="${id}">
                    停止
                </button>
                <button
                    class="action-button action-reclaim"
                    data-action="reclaim"
                    data-id="${id}">
                    回收
                </button>
            </div>
        `;
    }

    if (status === "STOPPED") {
        return `
            <div class="action-group">
                <button
                    class="action-button action-restart"
                    data-action="restart"
                    data-id="${id}">
                    重新启动
                </button>
                <button
                    class="action-button action-reclaim"
                    data-action="reclaim"
                    data-id="${id}">
                    回收
                </button>
            </div>
        `;
    }

    if (status === "FAILED" || status === "EXPIRED") {
        return `
            <div class="action-group">
                <button
                    class="action-button action-restart"
                    data-action="restart"
                    data-id="${id}">
                    尝试启动
                </button>
                <button
                    class="action-button action-reclaim"
                    data-action="reclaim"
                    data-id="${id}">
                    回收
                </button>
            </div>
        `;
    }

    return '<span class="action-disabled">暂无操作</span>';
}

function renderLabRows(labs) {
    const tbody = document.getElementById("labTableBody");

    if (!labs.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="11" class="empty-cell">
                    暂无靶场实例记录
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = labs.map(lab => {
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
                <td>${renderActions(lab)}</td>
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

function showError(message) {
    const errorMessage = document.getElementById("errorMessage");
    const successMessage = document.getElementById("successMessage");

    successMessage.hidden = true;
    errorMessage.textContent = message;
    errorMessage.hidden = false;
}

function showSuccess(message) {
    const errorMessage = document.getElementById("errorMessage");
    const successMessage = document.getElementById("successMessage");

    errorMessage.hidden = true;
    successMessage.textContent = message;
    successMessage.hidden = false;
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
                <td colspan="11" class="empty-cell">
                    实例数据加载失败
                </td>
            </tr>
        `;

    } finally {
        refreshButton.disabled = false;
    }
}

/**
 * 管理员实例操作事件
 */
async function handleLabAction(event) {
    const button = event.target.closest("button[data-action][data-id]");

    if (!button) {
        return;
    }

    const id = button.dataset.id;
    const action = button.dataset.action;

    const actionConfig = {
        stop: {
            title: "确定停止该实例吗？",
            message: "停止后 Web 和数据库容器将停止，但容器、网络和数据库数据卷会保留。",
            success: "实例已停止"
        },
        restart: {
            title: "确定重新启动该实例吗？",
            message: "系统将使用原来的端口和数据库数据卷重新启动该实例。",
            success: "实例重新启动成功"
        },
        reclaim: {
            title: "确定回收该实例吗？",
            message: "回收将删除该实例的容器和网络，但会保留数据库数据卷和实例记录。之后仍可尝试重新启动。请确认。",
            success: "实例容器和网络已回收，数据卷已保留"
        }
    };

    const config = actionConfig[action];

    if (!config) {
        return;
    }

    if (!window.confirm(`${config.title}\n\n${config.message}`)) {
        return;
    }

    const actionButtons = document.querySelectorAll(
        `button[data-id="${CSS.escape(id)}"]`
    );

    actionButtons.forEach(item => {
        item.disabled = true;
    });

    try {
        await requestLabAction(id, action);

        showSuccess(config.success);

        await loadLabList();

    } catch (error) {
        console.error("靶场实例操作失败：", error);
        showError(error.message || "靶场实例操作失败");

    } finally {
        actionButtons.forEach(item => {
            item.disabled = false;
        });
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("refreshButton")
        .addEventListener("click", loadLabList);

    document.getElementById("labTableBody")
        .addEventListener("click", handleLabAction);

    loadLabList();
});
