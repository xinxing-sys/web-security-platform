const API_BASE = "/api/admin/experiments";
const CATEGORY_API = "/api/categories";

let currentPage = 1;
let pageSize = 10;
let totalPages = 1;
let totalRecords = 0;
let categories = [];

const $ = id => document.getElementById(id);

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);
}

function showToast(message) {
    const toast = $("toast");
    toast.textContent = message;
    toast.classList.add("show");

    setTimeout(() => {
        toast.classList.remove("show");
    }, 2600);
}

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
    } catch {
        throw new Error("服务器返回的数据格式不正确");
    }

    if (!response.ok || result.success === false) {
        throw new Error(result.message || `请求失败：${response.status}`);
    }

    return result.data;
}

function difficultyText(value) {
    const map = {
        BEGINNER: "入门",
        INTERMEDIATE: "中级",
        ADVANCED: "高级"
    };
    return map[value] || value || "--";
}

function formatDate(value) {
    if (!value) return "--";

    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0] = value;
        return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")} ${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
    }

    return String(value).replace("T", " ").substring(0, 16);
}

async function loadCategories() {
    const data = await requestJson(CATEGORY_API);
    categories = Array.isArray(data) ? data : [];

    // 实验管理只允许选择实验类型分类
    const experimentCategories = categories.filter(item =>
        item.type === "EXPERIMENT"
    );

    const filter = $("categoryFilter");
    const formSelect = $("categoryId");

    filter.innerHTML = '<option value="">全部分类</option>';
    formSelect.innerHTML = '<option value="">请选择实验分类</option>';

    experimentCategories.forEach(item => {
        const option1 = document.createElement("option");
        option1.value = item.id;
        option1.textContent = item.name;
        filter.appendChild(option1);

        const option2 = document.createElement("option");
        option2.value = item.id;
        option2.textContent = item.name;
        formSelect.appendChild(option2);
    });
}

function getCategoryName(categoryId) {
    const item = categories.find(category =>
        String(category.id) === String(categoryId)
    );
    return item ? item.name : `分类 ${categoryId ?? "--"}`;
}

async function loadStatistics() {
    try {
        const [all, enabled, disabled] = await Promise.all([
            requestJson(`${API_BASE}?pageNum=1&pageSize=1`),
            requestJson(`${API_BASE}?pageNum=1&pageSize=1&status=1`),
            requestJson(`${API_BASE}?pageNum=1&pageSize=1&status=0`)
        ]);

        $("totalCount").textContent = all.total ?? 0;
        $("enabledCount").textContent = enabled.total ?? 0;
        $("disabledCount").textContent = disabled.total ?? 0;
    } catch (error) {
        console.error("实验统计加载失败：", error);
        $("totalCount").textContent = "--";
        $("enabledCount").textContent = "--";
        $("disabledCount").textContent = "--";
    }
}

function buildQuery() {
    const params = new URLSearchParams();

    params.set("pageNum", currentPage);
    params.set("pageSize", pageSize);

    const keyword = $("keyword").value.trim();
    const categoryId = $("categoryFilter").value;
    const difficulty = $("difficultyFilter").value;
    const status = $("statusFilter").value;

    if (keyword) params.set("keyword", keyword);
    if (categoryId) params.set("categoryId", categoryId);
    if (difficulty) params.set("difficulty", difficulty);
    if (status !== "") params.set("status", status);

    return params.toString();
}

async function loadExperiments() {
    const tbody = $("experimentTable");
    tbody.innerHTML = '<tr><td colspan="8" class="empty">正在加载...</td></tr>';

    try {
        const data = await requestJson(`${API_BASE}?${buildQuery()}`);

        const records = data.records || [];
        totalRecords = data.total || 0;
        totalPages = Math.max(1, Number(data.totalPages || 1));
        currentPage = Number(data.pageNum || currentPage);

        renderTable(records);

        $("pageInfo").textContent = `共 ${totalRecords} 条记录`;
        $("currentPage").textContent = `${currentPage} / ${totalPages}`;
        $("prevPage").disabled = currentPage <= 1;
        $("nextPage").disabled = currentPage >= totalPages;
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty">${escapeHtml(error.message)}</td></tr>`;
        showToast(error.message);
    }
}

function renderTable(records) {
    const tbody = $("experimentTable");

    if (!records.length) {
        tbody.innerHTML = '<tr><td colspan="8" class="empty">暂无实验资源</td></tr>';
        return;
    }

    tbody.innerHTML = records.map(item => `
        <tr>
            <td>${escapeHtml(item.id)}</td>
            <td>${escapeHtml(item.name)}</td>
            <td>${escapeHtml(getCategoryName(item.categoryId))}</td>
            <td><span class="difficulty">${escapeHtml(difficultyText(item.difficulty))}</span></td>
            <td>${escapeHtml(item.platformType || "--")}</td>
            <td>
                <span class="status ${Number(item.status) === 1 ? "enabled" : "disabled"}">
                    ${Number(item.status) === 1 ? "启用" : "停用"}
                </span>
            </td>
            <td>${escapeHtml(formatDate(item.updatedAt || item.createdAt))}</td>
            <td>
                <div class="action-group">
                    <button class="action-btn" data-action="edit" data-id="${escapeHtml(item.id)}">编辑</button>
                    <button class="action-btn delete" data-action="delete" data-id="${escapeHtml(item.id)}">删除</button>
                </div>
            </td>
        </tr>
    `).join("");
}

function openModal(title) {
    $("modalTitle").textContent = title;
    $("experimentModal").classList.add("show");
}

function closeModal() {
    $("experimentModal").classList.remove("show");
}

function resetForm() {
    $("experimentForm").reset();
    $("experimentId").value = "";
    $("platformType").value = "WEBGOAT";
    $("difficulty").value = "BEGINNER";
    $("status").value = "1";
}

function openAddModal() {
    resetForm();
    openModal("新增实验");
}

async function openEditModal(id) {
    try {
        const item = await requestJson(`${API_BASE}/${id}`);

        if (!item) {
            throw new Error("未找到该实验");
        }

        resetForm();

        $("experimentId").value = item.id ?? "";
        $("name").value = item.name ?? "";
        $("categoryId").value = item.categoryId ?? "";
        $("difficulty").value = item.difficulty ?? "BEGINNER";
        $("platformType").value = item.platformType ?? "WEBGOAT";
        $("target").value = item.target ?? "";
        $("description").value = item.description ?? "";
        $("steps").value = item.steps ?? "";
        $("platformUrl").value = item.platformUrl ?? "";
        $("lessonCode").value = item.lessonCode ?? "";
        $("status").value = String(item.status ?? 1);

        openModal("编辑实验");
    } catch (error) {
        showToast(error.message);
    }
}

function getFormData() {
    const name = $("name").value.trim();
    const categoryId = $("categoryId").value;

    if (!name) {
        throw new Error("请填写实验名称");
    }

    if (!categoryId) {
        throw new Error("请选择实验分类");
    }

    return {
        categoryId: Number(categoryId),
        name,
        difficulty: $("difficulty").value,
        target: $("target").value.trim(),
        description: $("description").value.trim(),
        steps: $("steps").value.trim(),
        platformType: $("platformType").value.trim() || "WEBGOAT",
        platformUrl: $("platformUrl").value.trim(),
        lessonCode: $("lessonCode").value.trim(),
        status: Number($("status").value)
    };
}

async function saveExperiment(event) {
    event.preventDefault();

    const id = $("experimentId").value;
    const saveBtn = $("saveBtn");

    try {
        const payload = getFormData();

        saveBtn.disabled = true;
        saveBtn.textContent = "正在保存...";

        if (id) {
            await requestJson(`${API_BASE}/${id}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showToast("实验修改成功");
        } else {
            await requestJson(API_BASE, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showToast("实验新增成功");
        }

        closeModal();
        await Promise.all([loadExperiments(), loadStatistics()]);
    } catch (error) {
        showToast(error.message);
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = "保存实验";
    }
}

async function deleteExperiment(id) {
    const confirmed = confirm(
        `确定删除 ID 为 ${id} 的实验吗？\n\n注意：删除操作会真实修改数据库。已有学生任务的实验可能无法删除。`
    );

    if (!confirmed) return;

    try {
        await requestJson(`${API_BASE}/${id}`, {
            method: "DELETE"
        });

        showToast("实验删除成功");

        await Promise.all([
            loadExperiments(),
            loadStatistics()
        ]);
    } catch (error) {
        showToast(error.message);
    }
}

function bindEvents() {
    $("addExperimentBtn").addEventListener("click", openAddModal);
    $("closeModal").addEventListener("click", closeModal);
    $("cancelModal").addEventListener("click", closeModal);

    $("experimentForm").addEventListener("submit", saveExperiment);

    $("searchBtn").addEventListener("click", () => {
        currentPage = 1;
        loadExperiments();
    });

    $("keyword").addEventListener("keydown", event => {
        if (event.key === "Enter") {
            currentPage = 1;
            loadExperiments();
        }
    });

    $("resetBtn").addEventListener("click", () => {
        $("keyword").value = "";
        $("categoryFilter").value = "";
        $("difficultyFilter").value = "";
        $("statusFilter").value = "";
        currentPage = 1;
        loadExperiments();
    });

    $("pageSize").addEventListener("change", () => {
        pageSize = Number($("pageSize").value);
        currentPage = 1;
        loadExperiments();
    });

    $("prevPage").addEventListener("click", () => {
        if (currentPage > 1) {
            currentPage--;
            loadExperiments();
        }
    });

    $("nextPage").addEventListener("click", () => {
        if (currentPage < totalPages) {
            currentPage++;
            loadExperiments();
        }
    });

    $("experimentTable").addEventListener("click", event => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;

        const id = button.dataset.id;

        if (button.dataset.action === "edit") {
            openEditModal(id);
        } else if (button.dataset.action === "delete") {
            deleteExperiment(id);
        }
    });

    $("experimentModal").addEventListener("click", event => {
        if (event.target === $("experimentModal")) {
            closeModal();
        }
    });
}

async function init() {
    bindEvents();

    try {
        await loadCategories();
    } catch (error) {
        console.error("分类加载失败：", error);
        showToast("实验分类加载失败，请检查分类接口");
    }

    await Promise.all([
        loadExperiments(),
        loadStatistics()
    ]);
}

document.addEventListener("DOMContentLoaded", init);