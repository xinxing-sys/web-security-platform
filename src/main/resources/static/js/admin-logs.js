/**
 * 管理员操作日志页面
 */

const API_BASE = "/api/admin/logs";

let currentPage = 1;
let currentPageSize = 10;
let totalPages = 1;
let toastTimer = null;

const elements = {
    username: document.getElementById("usernameInput"),
    operation: document.getElementById("operationInput"),
    startTime: document.getElementById("startTime"),
    endTime: document.getElementById("endTime"),
    pageSize: document.getElementById("pageSize"),
    tableBody: document.getElementById("logTableBody"),
    resultText: document.getElementById("resultText"),
    pageInfo: document.getElementById("pageInfo"),
    prevButton: document.getElementById("prevButton"),
    nextButton: document.getElementById("nextButton"),
    totalLogs: document.getElementById("totalLogs"),
    currentLogs: document.getElementById("currentLogs"),
    currentPage: document.getElementById("currentPage"),
    errorMessage: document.getElementById("errorMessage"),
    toast: document.getElementById("toast")
};

/**
 * 页面初始化
 */
document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("searchButton")
        .addEventListener("click", searchLogs);

    document.getElementById("resetButton")
        .addEventListener("click", resetFilters);

    elements.pageSize.addEventListener("change", () => {
        currentPageSize = Number(elements.pageSize.value);
        currentPage = 1;
        loadLogs();
    });

    elements.prevButton.addEventListener("click", () => {
        if (currentPage > 1) {
            currentPage--;
            loadLogs();
        }
    });

    elements.nextButton.addEventListener("click", () => {
        if (currentPage < totalPages) {
            currentPage++;
            loadLogs();
        }
    });

    // 支持在输入框中按 Enter 查询
    [
        elements.username,
        elements.operation
    ].forEach(input => {
        input.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                searchLogs();
            }
        });
    });

    loadLogs();
});

/**
 * 请求 JSON 接口
 */
async function requestJson(url) {
    const response = await fetch(url, {
        method: "GET",
        credentials: "same-origin",
        headers: {
            "Accept": "application/json"
        }
    });

    if (response.status === 401 || response.status === 403) {
        window.location.href = "/login";
        throw new Error("登录状态已失效，请重新登录");
    }

    const result = await response.json();

    if (!response.ok || !result.success) {
        throw new Error(result.message || "请求失败");
    }

    return result.data;
}

/**
 * 加载日志
 */
async function loadLogs() {
    elements.errorMessage.classList.add("hidden");

    elements.tableBody.innerHTML = `
        <tr>
            <td colspan="10" class="empty-cell">正在加载日志...</td>
        </tr>
    `;

    elements.resultText.textContent = "正在查询...";
    elements.prevButton.disabled = true;
    elements.nextButton.disabled = true;

    const params = new URLSearchParams();

    params.set("pageNum", String(currentPage));
    params.set("pageSize", String(currentPageSize));

    const username = elements.username.value.trim();
    const operation = elements.operation.value.trim();
    const startTime = elements.startTime.value;
    const endTime = elements.endTime.value;

    if (username) {
        params.set("username", username);
    }

    if (operation) {
        params.set("operation", operation);
    }

    if (startTime) {
        params.set("startTime", startTime);
    }

    if (endTime) {
        params.set("endTime", endTime);
    }

    try {
        const data = await requestJson(
            `${API_BASE}?${params.toString()}`
        );

        const records = Array.isArray(data.records)
            ? data.records
            : [];

        const total = Number(data.total || 0);

        totalPages = Math.max(1, Number(data.totalPages || 1));
        currentPage = Number(data.pageNum || currentPage);

        elements.totalLogs.textContent = total;
        elements.currentLogs.textContent = records.length;
        elements.currentPage.textContent = currentPage;

        elements.resultText.textContent =
            `共查询到 ${total} 条操作日志`;

        elements.pageInfo.textContent =
            `第 ${currentPage} 页，共 ${totalPages} 页`;

        elements.prevButton.disabled = currentPage <= 1;
        elements.nextButton.disabled = currentPage >= totalPages;

        renderLogs(records);

    } catch (error) {
        console.error("加载操作日志失败：", error);

        elements.tableBody.innerHTML = `
            <tr>
                <td colspan="10" class="empty-cell">
                    日志加载失败
                </td>
            </tr>
        `;

        elements.resultText.textContent = "查询失败";
        elements.totalLogs.textContent = "--";
        elements.currentLogs.textContent = "--";
        elements.currentPage.textContent = "--";

        elements.errorMessage.textContent =
            error.message || "加载操作日志失败";

        elements.errorMessage.classList.remove("hidden");
    }
}

/**
 * 渲染日志表格
 */
function renderLogs(records) {
    elements.tableBody.innerHTML = "";

    if (!records || records.length === 0) {
        elements.tableBody.innerHTML = `
            <tr>
                <td colspan="10" class="empty-cell">
                    暂无符合条件的操作日志
                </td>
            </tr>
        `;
        return;
    }

    records.forEach(item => {
        const row = document.createElement("tr");

        const role = getRoleInfo(item.role);
        const status = getResponseStatusInfo(item.responseStatus);

        row.innerHTML = `
            <td>#${escapeHtml(item.id)}</td>

            <td>${escapeHtml(item.username || "匿名用户")}</td>

            <td>
                <span class="role-tag ${role.className}">
                    ${role.text}
                </span>
            </td>

            <td>${escapeHtml(item.operationDesc || "-")}</td>

            <td>
                <span class="method-tag">
                    ${escapeHtml(item.httpMethod || "-")}
                </span>
            </td>

            <td class="uri-cell"
                title="${escapeHtml(item.requestUri || "")}">
                ${escapeHtml(item.requestUri || "-")}
            </td>

            <td>${escapeHtml(item.ipAddress || "-")}</td>

            <td>
                <span class="status-tag ${status.className}">
                    ${escapeHtml(status.text)}
                </span>
            </td>

            <td>${escapeHtml(item.durationMs ?? "-")} ms</td>

            <td>${escapeHtml(formatDate(item.createdAt))}</td>
        `;

        elements.tableBody.appendChild(row);
    });
}

/**
 * 用户角色显示
 */
function getRoleInfo(role) {
    const roleMap = {
        ADMIN: {
            text: "管理员",
            className: "role-admin"
        },
        STUDENT: {
            text: "学生",
            className: "role-student"
        }
    };

    return roleMap[role] || {
        text: role || "未知",
        className: "role-unknown"
    };
}

/**
 * HTTP 响应状态显示
 */
function getResponseStatusInfo(status) {
    const value = Number(status);

    if (value >= 200 && value < 300) {
        return {
            text: String(value),
            className: "status-success"
        };
    }

    if (value >= 400 && value < 500) {
        return {
            text: String(value),
            className: "status-warning"
        };
    }

    if (value >= 500) {
        return {
            text: String(value),
            className: "status-danger"
        };
    }

    return {
        text: status === null || status === undefined
            ? "-"
            : String(status),
        className: ""
    };
}

/**
 * 查询日志
 */
function searchLogs() {
    const startTime = elements.startTime.value;
    const endTime = elements.endTime.value;

    if (startTime && endTime && startTime > endTime) {
        showToast("开始时间不能晚于结束时间");
        return;
    }

    currentPage = 1;
    loadLogs();
}

/**
 * 重置筛选条件
 */
function resetFilters() {
    elements.username.value = "";
    elements.operation.value = "";
    elements.startTime.value = "";
    elements.endTime.value = "";

    currentPage = 1;
    loadLogs();
}

/**
 * 格式化日期
 */
function formatDate(value) {
    if (!value) {
        return "-";
    }

    if (Array.isArray(value)) {
        const [
            year,
            month,
            day,
            hour = 0,
            minute = 0,
            second = 0
        ] = value;

        return `${year}-${padZero(month)}-${padZero(day)} ` +
            `${padZero(hour)}:${padZero(minute)}:${padZero(second)}`;
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.getFullYear() + "-" +
        padZero(date.getMonth() + 1) + "-" +
        padZero(date.getDate()) + " " +
        padZero(date.getHours()) + ":" +
        padZero(date.getMinutes()) + ":" +
        padZero(date.getSeconds());
}

function padZero(value) {
    return String(value).padStart(2, "0");
}

/**
 * HTML 转义，避免日志内容被当作 HTML 执行
 */
function escapeHtml(value) {
    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * 显示提示
 */
function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.add("show");

    if (toastTimer) {
        window.clearTimeout(toastTimer);
    }

    toastTimer = window.setTimeout(() => {
        elements.toast.classList.remove("show");
    }, 2500);
}