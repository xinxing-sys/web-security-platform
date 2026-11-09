
const API_BASE = "/api/admin/users";

let currentPage = 1;
let currentPageSize = 10;
let totalPages = 1;
let currentDetailUserId = null;
let toastTimer = null;

const elements = {
    username: document.getElementById("usernameInput"),
    nickname: document.getElementById("nicknameInput"),
    role: document.getElementById("roleFilter"),
    status: document.getElementById("statusFilter"),
    pageSize: document.getElementById("pageSize"),
    tableBody: document.getElementById("userTableBody"),
    resultText: document.getElementById("resultText"),
    pageInfo: document.getElementById("pageInfo"),
    prevButton: document.getElementById("prevButton"),
    nextButton: document.getElementById("nextButton"),
    modal: document.getElementById("detailModal"),
    detailContent: document.getElementById("detailContent"),
    detailRole: document.getElementById("detailRole"),
    saveRoleButton: document.getElementById("saveRoleButton"),
    toast: document.getElementById("toast")
};

/**
 * 显示提示信息
 */
function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.add("show");

    if (toastTimer) {
        clearTimeout(toastTimer);
    }

    toastTimer = setTimeout(() => {
        elements.toast.classList.remove("show");
    }, 2600);
}

/**
 * HTML 转义，避免用户数据被当作 HTML 执行
 */
function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

/**
 * 统一处理 JSON 接口响应
 */
async function requestJson(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        }
    });

    let result;

    try {
        result = await response.json();
    } catch (error) {
        throw new Error("服务器返回的数据格式不正确");
    }

    if (!response.ok) {
        throw new Error(result.message || `请求失败：${response.status}`);
    }

    if (!result || result.success !== true) {
        throw new Error(result?.message || "操作失败");
    }

    return result.data;
}

/**
 * 日期格式化
 * 兼容 Java LocalDateTime 数组和字符串
 */
function formatDate(value) {
    if (!value) {
        return "-";
    }

    let date;

    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        date = new Date(year, month - 1, day, hour, minute, second);
    } else {
        date = new Date(value);
    }

    if (Number.isNaN(date.getTime())) {
        return Array.isArray(value) ? value.join("-") : String(value);
    }

    const pad = number => String(number).padStart(2, "0");

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
        `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/**
 * 角色显示
 */
function roleLabel(role) {
    return role === "ADMIN" ? "管理员" : "学生";
}

/**
 * 状态显示
 */
function statusLabel(status) {
    return Number(status) === 1 ? "启用" : "禁用";
}

/**
 * 加载顶部统计数据
 *
 * 分别查询全部、启用、禁用用户的总数。
 * 每次只取一条记录，使用后端返回的 total 统计。
 */
async function loadStatistics() {
    try {
        const [allData, enabledData, disabledData] = await Promise.all([
            requestJson(`${API_BASE}?pageNum=1&pageSize=1`),
            requestJson(`${API_BASE}?pageNum=1&pageSize=1&status=1`),
            requestJson(`${API_BASE}?pageNum=1&pageSize=1&status=0`)
        ]);

        document.getElementById("totalUsers").textContent =
            allData.total ?? 0;

        document.getElementById("enabledUsers").textContent =
            enabledData.total ?? 0;

        document.getElementById("disabledUsers").textContent =
            disabledData.total ?? 0;

    } catch (error) {
        console.error("加载用户统计失败：", error);
        document.getElementById("totalUsers").textContent = "--";
        document.getElementById("enabledUsers").textContent = "--";
        document.getElementById("disabledUsers").textContent = "--";
    }
}

/**
 * 获取当前筛选条件
 */
function buildQueryParams() {
    const params = new URLSearchParams();

    params.set("pageNum", currentPage);
    params.set("pageSize", currentPageSize);

    const username = elements.username.value.trim();
    const nickname = elements.nickname.value.trim();
    const role = elements.role.value;
    const status = elements.status.value;

    if (username) {
        params.set("username", username);
    }

    if (nickname) {
        params.set("nickname", nickname);
    }

    if (role) {
        params.set("role", role);
    }

    if (status !== "") {
        params.set("status", status);
    }

    return params;
}

/**
 * 加载用户列表
 */
async function loadUsers() {
    elements.tableBody.innerHTML = `
        <tr>
            <td colspan="8" class="empty-cell">正在加载用户...</td>
        </tr>
    `;

    elements.resultText.textContent = "正在查询...";

    try {
        const params = buildQueryParams();
        const data = await requestJson(`${API_BASE}?${params.toString()}`);

        const records = data.records || [];

        totalPages = Math.max(1, Number(data.totalPages || 1));

        renderUserTable(records);

        elements.resultText.textContent =
            `共 ${data.total ?? 0} 位用户`;

        elements.pageInfo.textContent =
            `第 ${data.pageNum || currentPage} 页 / 共 ${totalPages} 页`;

        elements.prevButton.disabled = currentPage <= 1;
        elements.nextButton.disabled = currentPage >= totalPages;

    } catch (error) {
        console.error("加载用户列表失败：", error);

        elements.tableBody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-cell">
                    ${escapeHtml(error.message)}
                </td>
            </tr>
        `;

        elements.resultText.textContent = "用户列表加载失败";
        elements.pageInfo.textContent = "-";

        showToast(error.message);
    }
}

/**
 * 渲染用户表格
 */
function renderUserTable(users) {
    if (!users || users.length === 0) {
        elements.tableBody.innerHTML = `
            <tr>
                <td colspan="8" class="empty-cell">
                    暂无符合条件的用户
                </td>
            </tr>
        `;
        return;
    }

    elements.tableBody.innerHTML = users.map(user => {
        const roleClass = user.role === "ADMIN"
            ? "role-admin"
            : "role-student";

        const statusClass = Number(user.status) === 1
            ? "status-enabled"
            : "status-disabled";

        const nextStatus = Number(user.status) === 1 ? 0 : 1;
        const statusAction = nextStatus === 0 ? "禁用" : "启用";
        const actionClass = nextStatus === 0 ? "danger" : "success";

        return `
            <tr>
                <td>${escapeHtml(user.id)}</td>
                <td>${escapeHtml(user.username)}</td>
                <td>${escapeHtml(user.nickname || "-")}</td>
                <td>${escapeHtml(user.email || "-")}</td>
                <td>
                    <span class="role-tag ${roleClass}">
                        ${roleLabel(user.role)}
                    </span>
                </td>
                <td>
                    <span class="status-tag ${statusClass}">
                        ${statusLabel(user.status)}
                    </span>
                </td>
                <td>${escapeHtml(formatDate(user.createdAt))}</td>
                <td>
                    <div class="action-group">
                        <button
                            type="button"
                            class="action-button"
                            data-action="detail"
                            data-id="${escapeHtml(user.id)}">
                            详情
                        </button>
                        <button
                            type="button"
                            class="action-button ${actionClass}"
                            data-action="status"
                            data-id="${escapeHtml(user.id)}"
                            data-status="${nextStatus}">
                            ${statusAction}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

/**
 * 查看用户详情
 */
async function openUserDetail(userId) {
    currentDetailUserId = userId;

    elements.modal.classList.add("show");
    elements.detailContent.innerHTML =
        `<div class="empty-cell">正在加载用户详情...</div>`;

    elements.detailRole.disabled = true;
    elements.saveRoleButton.disabled = true;

    try {
        const user = await requestJson(`${API_BASE}/${userId}`);

        elements.detailContent.innerHTML = `
            <div class="detail-grid">
                <div class="detail-item">
                    <span>用户 ID</span>
                    <strong>${escapeHtml(user.id)}</strong>
                </div>
                <div class="detail-item">
                    <span>用户名</span>
                    <strong>${escapeHtml(user.username)}</strong>
                </div>
                <div class="detail-item">
                    <span>昵称</span>
                    <strong>${escapeHtml(user.nickname || "-")}</strong>
                </div>
                <div class="detail-item">
                    <span>邮箱</span>
                    <strong>${escapeHtml(user.email || "-")}</strong>
                </div>
                <div class="detail-item">
                    <span>用户角色</span>
                    <strong>${roleLabel(user.role)}</strong>
                </div>
                <div class="detail-item">
                    <span>账号状态</span>
                    <strong>${statusLabel(user.status)}</strong>
                </div>
                <div class="detail-item">
                    <span>注册时间</span>
                    <strong>${escapeHtml(formatDate(user.createdAt))}</strong>
                </div>
                <div class="detail-item">
                    <span>最近更新时间</span>
                    <strong>${escapeHtml(formatDate(user.updatedAt))}</strong>
                </div>
            </div>
        `;

        elements.detailRole.value = user.role;
        elements.detailRole.disabled = false;
        elements.saveRoleButton.disabled = false;

    } catch (error) {
        console.error("获取用户详情失败：", error);

        elements.detailContent.innerHTML = `
            <div class="empty-cell">
                ${escapeHtml(error.message)}
            </div>
        `;

        showToast(error.message);
    }
}

/**
 * 修改用户启用/禁用状态
 */
async function updateUserStatus(userId, status) {
    const action = Number(status) === 1 ? "启用" : "禁用";

    const confirmed = window.confirm(
        `确定要${action}用户 ID ${userId} 吗？`
    );

    if (!confirmed) {
        return;
    }

    try {
        await requestJson(`${API_BASE}/${userId}/status`, {
            method: "PUT",
            body: JSON.stringify({
                status: Number(status)
            })
        });

        showToast(`用户${action}成功`);

        await Promise.all([
            loadUsers(),
            loadStatistics()
        ]);

    } catch (error) {
        console.error("修改用户状态失败：", error);
        showToast(error.message);
    }
}

/**
 * 修改用户角色
 */
async function saveUserRole() {
    if (!currentDetailUserId) {
        showToast("请先选择用户");
        return;
    }

    const role = elements.detailRole.value;

    const confirmed = window.confirm(
        `确定将该用户角色修改为“${roleLabel(role)}”吗？`
    );

    if (!confirmed) {
        return;
    }

    elements.saveRoleButton.disabled = true;

    try {
        await requestJson(`${API_BASE}/${currentDetailUserId}/role`, {
            method: "PUT",
            body: JSON.stringify({
                role: role
            })
        });

        showToast("用户角色修改成功");

        await Promise.all([
            loadUsers(),
            loadStatistics()
        ]);

        await openUserDetail(currentDetailUserId);

    } catch (error) {
        console.error("修改用户角色失败：", error);
        showToast(error.message);
    } finally {
        elements.saveRoleButton.disabled = false;
    }
}

/**
 * 关闭详情弹窗
 */
function closeModal() {
    elements.modal.classList.remove("show");
    currentDetailUserId = null;
}

/**
 * 查询按钮
 */
document.getElementById("searchButton").addEventListener("click", () => {
    currentPage = 1;
    loadUsers();
});

/**
 * 重置筛选
 */
document.getElementById("resetButton").addEventListener("click", () => {
    elements.username.value = "";
    elements.nickname.value = "";
    elements.role.value = "";
    elements.status.value = "";

    currentPage = 1;
    loadUsers();
});

/**
 * 回车执行查询
 */
[elements.username, elements.nickname].forEach(input => {
    input.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            currentPage = 1;
            loadUsers();
        }
    });
});

/**
 * 修改每页数量
 */
elements.pageSize.addEventListener("change", () => {
    currentPageSize = Number(elements.pageSize.value);
    currentPage = 1;
    loadUsers();
});

/**
 * 上一页
 */
elements.prevButton.addEventListener("click", () => {
    if (currentPage > 1) {
        currentPage--;
        loadUsers();
    }
});

/**
 * 下一页
 */
elements.nextButton.addEventListener("click", () => {
    if (currentPage < totalPages) {
        currentPage++;
        loadUsers();
    }
});

/**
 * 用户列表按钮事件委托
 */
elements.tableBody.addEventListener("click", event => {
    const button = event.target.closest("button[data-action]");

    if (!button) {
        return;
    }

    const userId = button.dataset.id;
    const action = button.dataset.action;

    if (action === "detail") {
        openUserDetail(userId);
    }

    if (action === "status") {
        updateUserStatus(userId, Number(button.dataset.status));
    }
});

/**
 * 角色修改
 */
elements.saveRoleButton.addEventListener("click", saveUserRole);

/**
 * 关闭弹窗
 */
document.getElementById("closeModal").addEventListener("click", closeModal);
document.getElementById("closeModalBottom").addEventListener("click", closeModal);

/**
 * 点击遮罩层关闭弹窗
 */
elements.modal.addEventListener("click", event => {
    if (event.target === elements.modal) {
        closeModal();
    }
});

/**
 * 页面初始化
 */
async function initializePage() {
    currentPageSize = Number(elements.pageSize.value);

    await Promise.all([
        loadStatistics(),
        loadUsers()
    ]);
}

initializePage();
