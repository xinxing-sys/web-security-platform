const VULNERABILITY_API = "/api/admin/vulnerabilities";
const CATEGORY_API = "/api/admin/categories";

let categories = [];

let vulnerabilityPage = 1;
let vulnerabilityPageSize = 10;
let vulnerabilityTotalPages = 1;

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

    setTimeout(() => toast.classList.remove("show"), 2600);
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

function formatDate(value) {
    if (!value) return "--";

    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0] = value;
        return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")} ${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
    }

    return String(value).replace("T", " ").substring(0, 16);
}

function categoryName(id) {
    const category = categories.find(item =>
        String(item.id) === String(id)
    );

    return category ? category.name : `分类 ${id ?? "--"}`;
}

function statusHtml(status) {
    const enabled = Number(status) === 1;

    return `<span class="status ${enabled ? "enabled" : "disabled"}">
        ${enabled ? "启用" : "停用"}
    </span>`;
}

/* =========================
   分类数据
========================= */

async function loadCategories() {
    const data = await requestJson(CATEGORY_API);
    categories = Array.isArray(data) ? data : [];

    renderCategoryFilters();
    renderCategoryTable();
}

function renderCategoryFilters() {
    const filter = $("vulnerabilityCategoryFilter");
    const formSelect = $("vulnerabilityCategoryId");

    const filterValue = filter.value;
    const formValue = formSelect.value;

    filter.innerHTML = '<option value="">全部分类</option>';
    formSelect.innerHTML = '<option value="">请选择分类</option>';

    categories.forEach(item => {
        const option1 = document.createElement("option");
        option1.value = item.id;
        option1.textContent = item.name;
        filter.appendChild(option1);

        const option2 = document.createElement("option");
        option2.value = item.id;
        option2.textContent = item.name;

        if (Number(item.status) !== 1) {
            option2.disabled = true;
            option2.textContent += "（已停用）";
        }

        formSelect.appendChild(option2);
    });

    filter.value = filterValue;
    formSelect.value = formValue;
}

function renderCategoryTable() {
    const tbody = $("categoryTable");

    if (!categories.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty">暂无分类</td></tr>';
        return;
    }

    tbody.innerHTML = categories.map(item => `
        <tr>
            <td>${escapeHtml(item.id)}</td>
            <td>${escapeHtml(item.name)}</td>
            <td>${escapeHtml(item.type || "--")}</td>
            <td>${escapeHtml(item.description || "--")}</td>
            <td>${escapeHtml(item.sortOrder ?? 0)}</td>
            <td>${statusHtml(item.status)}</td>
            <td>
                <div class="action-group">
                    <button class="action-btn"
                            data-action="edit-category"
                            data-id="${escapeHtml(item.id)}">编辑</button>
                    <button class="action-btn delete"
                            data-action="delete-category"
                            data-id="${escapeHtml(item.id)}">删除</button>
                </div>
            </td>
        </tr>
    `).join("");
}

/* =========================
   统计数据
========================= */

async function loadStatistics() {
    try {
        const [all, enabled] = await Promise.all([
            requestJson(`${VULNERABILITY_API}?pageNum=1&pageSize=1`),
            requestJson(`${VULNERABILITY_API}?pageNum=1&pageSize=1&status=1`)
        ]);

        $("vulnerabilityTotal").textContent = all.total ?? 0;
        $("vulnerabilityEnabled").textContent = enabled.total ?? 0;
        $("categoryTotal").textContent = categories.length;
    } catch (error) {
        console.error("统计数据加载失败：", error);
        $("vulnerabilityTotal").textContent = "--";
        $("vulnerabilityEnabled").textContent = "--";
        $("categoryTotal").textContent = categories.length;
    }
}

/* =========================
   漏洞知识列表
========================= */

function vulnerabilityQueryString() {
    const params = new URLSearchParams();

    params.set("pageNum", vulnerabilityPage);
    params.set("pageSize", vulnerabilityPageSize);

    const keyword = $("vulnerabilityKeyword").value.trim();
    const categoryId = $("vulnerabilityCategoryFilter").value;
    const status = $("vulnerabilityStatusFilter").value;

    if (keyword) params.set("keyword", keyword);
    if (categoryId) params.set("categoryId", categoryId);
    if (status !== "") params.set("status", status);

    return params.toString();
}

async function loadVulnerabilities() {
    const tbody = $("vulnerabilityTable");
    tbody.innerHTML = '<tr><td colspan="7" class="empty">正在加载...</td></tr>';

    try {
        const data = await requestJson(
            `${VULNERABILITY_API}?${vulnerabilityQueryString()}`
        );

        const records = data.records || [];
        vulnerabilityTotalPages = Math.max(
            1, Number(data.totalPages || 1)
        );
        vulnerabilityPage = Number(data.pageNum || vulnerabilityPage);

        renderVulnerabilities(records);

        $("vulnerabilityPageInfo").textContent =
            `共 ${data.total || 0} 条记录`;

        $("vulnerabilityPageText").textContent =
            `${vulnerabilityPage} / ${vulnerabilityTotalPages}`;

        $("vulnerabilityPrev").disabled = vulnerabilityPage <= 1;
        $("vulnerabilityNext").disabled =
            vulnerabilityPage >= vulnerabilityTotalPages;

    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty">${escapeHtml(error.message)}</td></tr>`;
        showToast(error.message);
    }
}

function renderVulnerabilities(records) {
    const tbody = $("vulnerabilityTable");

    if (!records.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty">暂无漏洞知识</td></tr>';
        return;
    }

    tbody.innerHTML = records.map(item => `
        <tr>
            <td>${escapeHtml(item.id)}</td>
            <td>${escapeHtml(item.title)}</td>
            <td>${escapeHtml(categoryName(item.categoryId))}</td>
            <td>${escapeHtml(item.summary || "--")}</td>
            <td>${statusHtml(item.status)}</td>
            <td>${escapeHtml(formatDate(item.createdAt))}</td>
            <td>
                <div class="action-group">
                    <button class="action-btn"
                            data-action="edit-vulnerability"
                            data-id="${escapeHtml(item.id)}">编辑</button>
                    <button class="action-btn delete"
                            data-action="delete-vulnerability"
                            data-id="${escapeHtml(item.id)}">删除</button>
                </div>
            </td>
        </tr>
    `).join("");
}

/* =========================
   漏洞知识新增、编辑
========================= */

function openVulnerabilityModal(title) {
    $("vulnerabilityModalTitle").textContent = title;
    $("vulnerabilityModal").classList.add("show");
}

function resetVulnerabilityForm() {
    $("vulnerabilityForm").reset();
    $("vulnerabilityId").value = "";
    $("vulnerabilityStatus").value = "1";
}

function openAddVulnerability() {
    resetVulnerabilityForm();
    openVulnerabilityModal("新增漏洞知识");
}

async function openEditVulnerability(id) {
    try {
        const item = await requestJson(`${VULNERABILITY_API}/${id}`);

        if (!item) throw new Error("漏洞知识不存在");

        resetVulnerabilityForm();

        $("vulnerabilityId").value = item.id ?? "";
        $("vulnerabilityTitle").value = item.title ?? "";
        $("vulnerabilityCategoryId").value = item.categoryId ?? "";
        $("vulnerabilitySummary").value = item.summary ?? "";
        $("vulnerabilityPrinciple").value = item.principle ?? "";
        $("vulnerabilityHarm").value = item.harm ?? "";
        $("vulnerabilityProtection").value = item.protection ?? "";
        $("vulnerabilityContent").value = item.content ?? "";
        $("vulnerabilityStatus").value = String(item.status ?? 1);

        openVulnerabilityModal("编辑漏洞知识");
    } catch (error) {
        showToast(error.message);
    }
}

function getVulnerabilityFormData() {
    const title = $("vulnerabilityTitle").value.trim();
    const categoryId = $("vulnerabilityCategoryId").value;

    if (!title) throw new Error("漏洞标题不能为空");
    if (title.length > 200) throw new Error("漏洞标题不能超过200个字符");
    if (!categoryId) throw new Error("请选择所属分类");

    return {
        categoryId: Number(categoryId),
        title,
        summary: $("vulnerabilitySummary").value.trim(),
        principle: $("vulnerabilityPrinciple").value.trim(),
        harm: $("vulnerabilityHarm").value.trim(),
        protection: $("vulnerabilityProtection").value.trim(),
        content: $("vulnerabilityContent").value.trim(),
        status: Number($("vulnerabilityStatus").value)
    };
}

async function saveVulnerability(event) {
    event.preventDefault();

    const id = $("vulnerabilityId").value;
    const button = $("saveVulnerabilityBtn");

    try {
        const payload = getVulnerabilityFormData();

        button.disabled = true;
        button.textContent = "正在保存...";

        if (id) {
            await requestJson(`${VULNERABILITY_API}/${id}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showToast("漏洞知识修改成功");
        } else {
            await requestJson(VULNERABILITY_API, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showToast("漏洞知识新增成功");
        }

        closeModal("vulnerabilityModal");

        await Promise.all([
            loadVulnerabilities(),
            loadStatistics()
        ]);

    } catch (error) {
        showToast(error.message);
    } finally {
        button.disabled = false;
        button.textContent = "保存";
    }
}

async function deleteVulnerability(id) {
    if (!confirm(`确定删除 ID 为 ${id} 的漏洞知识吗？此操作会真实修改数据库。`)) {
        return;
    }

    try {
        await requestJson(`${VULNERABILITY_API}/${id}`, {
            method: "DELETE"
        });

        showToast("漏洞知识删除成功");

        await Promise.all([
            loadVulnerabilities(),
            loadStatistics()
        ]);
    } catch (error) {
        showToast(error.message);
    }
}

/* =========================
   分类新增、编辑
========================= */

function openCategoryModal(title) {
    $("categoryModalTitle").textContent = title;
    $("categoryModal").classList.add("show");
}

function resetCategoryForm() {
    $("categoryForm").reset();
    $("categoryId").value = "";
    $("categorySortOrder").value = "0";
    $("categoryStatus").value = "1";
}

function openAddCategory() {
    resetCategoryForm();
    openCategoryModal("新增分类");
}

function openEditCategory(id) {
    const item = categories.find(category =>
        String(category.id) === String(id)
    );

    if (!item) {
        showToast("分类不存在");
        return;
    }

    resetCategoryForm();

    $("categoryId").value = item.id ?? "";
    $("categoryName").value = item.name ?? "";
    $("categoryType").value = item.type ?? "";
    $("categoryDescription").value = item.description ?? "";
    $("categorySortOrder").value = item.sortOrder ?? 0;
    $("categoryStatus").value = String(item.status ?? 1);

    openCategoryModal("编辑分类");
}

function getCategoryFormData() {
    const name = $("categoryName").value.trim();

    if (!name) throw new Error("分类名称不能为空");

    const sortOrder = Number($("categorySortOrder").value);

    if (!Number.isInteger(sortOrder)) {
        throw new Error("排序必须为整数");
    }

    return {
        name,
        type: $("categoryType").value.trim(),
        description: $("categoryDescription").value.trim(),
        sortOrder,
        status: Number($("categoryStatus").value)
    };
}

async function saveCategory(event) {
    event.preventDefault();

    const id = $("categoryId").value;
    const button = $("saveCategoryBtn");

    try {
        const payload = getCategoryFormData();

        button.disabled = true;
        button.textContent = "正在保存...";

        if (id) {
            await requestJson(`${CATEGORY_API}/${id}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showToast("分类修改成功");
        } else {
            await requestJson(CATEGORY_API, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showToast("分类新增成功");
        }

        closeModal("categoryModal");

        await refreshAll();

    } catch (error) {
        showToast(error.message);
    } finally {
        button.disabled = false;
        button.textContent = "保存";
    }
}

async function deleteCategory(id) {
    const item = categories.find(category =>
        String(category.id) === String(id)
    );

    if (!item) return;

    if (!confirm(
        `确定删除分类“${item.name}”吗？\n\n如果分类下还有漏洞知识，后端会拒绝删除。`
    )) {
        return;
    }

    try {
        await requestJson(`${CATEGORY_API}/${id}`, {
            method: "DELETE"
        });

        showToast("分类删除成功");
        await refreshAll();

    } catch (error) {
        showToast(error.message);
    }
}

/* =========================
   公共功能
========================= */

function closeModal(id) {
    $(id).classList.remove("show");
}

async function refreshAll() {
    await loadCategories();

    await Promise.all([
        loadVulnerabilities(),
        loadStatistics()
    ]);
}

function bindEvents() {
    document.querySelectorAll(".tab").forEach(button => {
        button.addEventListener("click", () => {
            document.querySelectorAll(".tab").forEach(tab =>
                tab.classList.remove("active")
            );

            button.classList.add("active");

            const isVulnerability =
                button.dataset.tab === "vulnerabilities";

            $("vulnerabilitiesPanel").classList.toggle(
                "hidden", !isVulnerability
            );

            $("categoriesPanel").classList.toggle(
                "hidden", isVulnerability
            );

            if (isVulnerability) {
                loadVulnerabilities();
            } else {
                loadCategories();
            }
        });
    });

    $("addVulnerabilityBtn").addEventListener(
        "click", openAddVulnerability
    );

    $("addCategoryBtn").addEventListener(
        "click", openAddCategory
    );

    $("vulnerabilityForm").addEventListener(
        "submit", saveVulnerability
    );

    $("categoryForm").addEventListener(
        "submit", saveCategory
    );

    $("searchVulnerabilityBtn").addEventListener("click", () => {
        vulnerabilityPage = 1;
        loadVulnerabilities();
    });

    $("vulnerabilityKeyword").addEventListener("keydown", event => {
        if (event.key === "Enter") {
            vulnerabilityPage = 1;
            loadVulnerabilities();
        }
    });

    $("resetVulnerabilityBtn").addEventListener("click", () => {
        $("vulnerabilityKeyword").value = "";
        $("vulnerabilityCategoryFilter").value = "";
        $("vulnerabilityStatusFilter").value = "";
        vulnerabilityPage = 1;
        loadVulnerabilities();
    });

    $("vulnerabilityPageSize").addEventListener("change", () => {
        vulnerabilityPageSize =
            Number($("vulnerabilityPageSize").value);
        vulnerabilityPage = 1;
        loadVulnerabilities();
    });

    $("vulnerabilityPrev").addEventListener("click", () => {
        if (vulnerabilityPage > 1) {
            vulnerabilityPage--;
            loadVulnerabilities();
        }
    });

    $("vulnerabilityNext").addEventListener("click", () => {
        if (vulnerabilityPage < vulnerabilityTotalPages) {
            vulnerabilityPage++;
            loadVulnerabilities();
        }
    });

    $("vulnerabilityTable").addEventListener("click", event => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;

        const id = button.dataset.id;

        if (button.dataset.action === "edit-vulnerability") {
            openEditVulnerability(id);
        } else if (button.dataset.action === "delete-vulnerability") {
            deleteVulnerability(id);
        }
    });

    $("categoryTable").addEventListener("click", event => {
        const button = event.target.closest("button[data-action]");
        if (!button) return;

        const id = button.dataset.id;

        if (button.dataset.action === "edit-category") {
            openEditCategory(id);
        } else if (button.dataset.action === "delete-category") {
            deleteCategory(id);
        }
    });

    document.querySelectorAll("[data-close]").forEach(button => {
        button.addEventListener("click", () => {
            closeModal(button.dataset.close);
        });
    });

    document.querySelectorAll(".modal-mask").forEach(mask => {
        mask.addEventListener("click", event => {
            if (event.target === mask) {
                closeModal(mask.id);
            }
        });
    });
}

async function init() {
    bindEvents();

    try {
        await refreshAll();
    } catch (error) {
        console.error("页面初始化失败：", error);
        showToast(error.message);
    }
}

document.addEventListener("DOMContentLoaded", init);