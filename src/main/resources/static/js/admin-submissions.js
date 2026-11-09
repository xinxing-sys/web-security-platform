
const API_BASE = "/api/admin/submissions";

let currentSubmissionId = null;
let currentSubmission = null;
let allSubmissions = [];

const elements = {
    tableBody: document.getElementById("submissionTableBody"),
    statusFilter: document.getElementById("statusFilter"),
    loading: document.getElementById("loading"),
    emptyState: document.getElementById("emptyState"),
    errorMessage: document.getElementById("errorMessage"),
    modal: document.getElementById("detailModal"),
    detailLoading: document.getElementById("detailLoading"),
    detailContent: document.getElementById("detailContent"),
    reviewForm: document.getElementById("reviewForm"),
    toast: document.getElementById("toast")
};

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    loadSubmissions();
});

function bindEvents() {
    elements.statusFilter.addEventListener("change", loadSubmissions);

    document.getElementById("refreshBtn")
        .addEventListener("click", loadSubmissions);

    document.getElementById("closeModalBtn")
        .addEventListener("click", closeModal);

    elements.modal.addEventListener("click", event => {
        if (event.target === elements.modal) {
            closeModal();
        }
    });

    document.getElementById("approveBtn")
        .addEventListener("click", () => submitReview("APPROVED"));

    document.getElementById("rejectBtn")
        .addEventListener("click", () => submitReview("REJECTED"));

    document.getElementById("downloadAttachmentBtn")
        .addEventListener("click", downloadAttachment);
}

/**
 * 请求后端 JSON 接口
 */
async function requestJson(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
        headers: {
            ...(options.headers || {})
        }
    });

    const result = await response.json();

    if (!response.ok || !result.success) {
        throw new Error(result.message || "请求失败");
    }

    return result.data;
}

/**
 * 加载报告列表
 */
async function loadSubmissions() {
    elements.loading.classList.remove("hidden");
    elements.emptyState.classList.add("hidden");
    elements.errorMessage.classList.add("hidden");
    elements.tableBody.innerHTML = "";

    const status = elements.statusFilter.value;

    let url = API_BASE;

    if (status) {
        url += "?status=" + encodeURIComponent(status);
    }

    try {
        const data = await requestJson(url);

        allSubmissions = Array.isArray(data) ? data : [];

        updateStatistics();
        renderSubmissionList(allSubmissions);

    } catch (error) {
        showError(error.message);
    } finally {
        elements.loading.classList.add("hidden");
    }
}

/**
 * 更新顶部统计数据
 *
 * 统计始终基于全部报告，而不是当前筛选结果。
 */
function updateStatistics() {
    const total = allSubmissions.length;

    const pending = allSubmissions.filter(
        item => item.reviewStatus === "PENDING"
    ).length;

    const approved = allSubmissions.filter(
        item => item.reviewStatus === "APPROVED"
    ).length;

    const rejected = allSubmissions.filter(
        item => item.reviewStatus === "REJECTED"
    ).length;

    document.getElementById("totalCount").textContent = total;
    document.getElementById("pendingCount").textContent = pending;
    document.getElementById("approvedCount").textContent = approved;
    document.getElementById("rejectedCount").textContent = rejected;
}

/**
 * 渲染报告列表
 */
function renderSubmissionList(submissions) {
    elements.tableBody.innerHTML = "";

    if (!submissions || submissions.length === 0) {
        elements.emptyState.classList.remove("hidden");
        return;
    }

    elements.emptyState.classList.add("hidden");

    submissions.forEach(item => {
        const row = document.createElement("tr");

        const statusClass = getStatusClass(item.reviewStatus);
        const statusText = getStatusText(item.reviewStatus);

        const score = item.score !== null && item.score !== undefined
            ? item.score
            : "未评分";

        row.innerHTML = `
            <td>#${escapeHtml(item.id)}</td>
            <td>${escapeHtml(item.userId)}</td>
            <td>${escapeHtml(item.taskId)}</td>
            <td>${escapeHtml(formatDate(item.submittedAt))}</td>
            <td>
                <span class="status-badge ${statusClass}">
                    ${statusText}
                </span>
            </td>
            <td>${escapeHtml(score)}</td>
            <td>
                <button class="btn btn-view" type="button">
                    查看详情
                </button>
            </td>
        `;

        row.querySelector("button").addEventListener("click", () => {
            openSubmissionDetail(item.id);
        });

        elements.tableBody.appendChild(row);
    });
}

/**
 * 打开报告详情
 */
async function openSubmissionDetail(id) {
    currentSubmissionId = id;
    currentSubmission = null;

    elements.modal.classList.remove("hidden");
    elements.detailLoading.classList.remove("hidden");
    elements.detailContent.classList.add("hidden");

    try {
        const data = await requestJson(`${API_BASE}/${id}`);

        currentSubmission = data;

        renderSubmissionDetail(data);

        elements.detailContent.classList.remove("hidden");

    } catch (error) {
        showToast(error.message);
        closeModal();
    } finally {
        elements.detailLoading.classList.add("hidden");
    }
}

/**
 * 渲染报告详情
 */
function renderSubmissionDetail(item) {
    setText("detailId", item.id);
    setText("detailUserId", item.userId);
    setText("detailTaskId", item.taskId);
    setText("detailSubmittedAt", formatDate(item.submittedAt));

    setText("detailStatus", getStatusText(item.reviewStatus));

    setText(
        "detailScore",
        item.score !== null && item.score !== undefined
            ? item.score + " 分"
            : "未评分"
    );

    setText(
        "detailReportContent",
        item.content || "学生未填写报告正文"
    );

    setText(
        "detailReviewerId",
        item.reviewerId !== null && item.reviewerId !== undefined
            ? item.reviewerId
            : "-"
    );

    setText(
        "detailReviewedAt",
        formatDate(item.reviewedAt)
    );

    setText(
        "detailReviewComment",
        item.reviewComment || "暂无评语"
    );

    const attachmentName = document.getElementById("attachmentName");
    const downloadButton = document.getElementById("downloadAttachmentBtn");

    if (item.attachmentPath) {
        attachmentName.textContent = "已上传实验报告附件";
        downloadButton.classList.remove("hidden");
    } else {
        attachmentName.textContent = "未上传附件";
        downloadButton.classList.add("hidden");
    }

    const isPending = item.reviewStatus === "PENDING";

    elements.reviewForm.classList.toggle("hidden", !isPending);

    document.getElementById("scoreInput").value =
        item.score !== null && item.score !== undefined
            ? item.score
            : "";

    document.getElementById("reviewCommentInput").value =
        item.reviewComment || "";
}

/**
 * 提交审核
 */
async function submitReview(reviewStatus) {
    if (!currentSubmissionId || !currentSubmission) {
        showToast("请先选择一份实验报告");
        return;
    }

    if (currentSubmission.reviewStatus !== "PENDING") {
        showToast("该报告已经审核，不能重复审核");
        return;
    }

    const scoreValue = document.getElementById("scoreInput").value;
    const reviewComment = document.getElementById("reviewCommentInput").value.trim();

    if (scoreValue === "") {
        showToast("请输入实验成绩");
        return;
    }

    const score = Number(scoreValue);

    if (!Number.isFinite(score) || score < 0 || score > 100) {
        showToast("成绩必须在 0 到 100 分之间");
        return;
    }

    const actionText = reviewStatus === "APPROVED"
        ? "通过"
        : "驳回";

    const confirmed = window.confirm(
        `确定要${actionText}这份实验报告吗？`
    );

    if (!confirmed) {
        return;
    }

    const approveButton = document.getElementById("approveBtn");
    const rejectButton = document.getElementById("rejectBtn");

    approveButton.disabled = true;
    rejectButton.disabled = true;

    try {
        await requestJson(`${API_BASE}/${currentSubmissionId}/review`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                reviewStatus: reviewStatus,
                score: score,
                reviewComment: reviewComment
            })
        });

        showToast(`报告${actionText}成功`);

        closeModal();

        await loadSubmissions();

    } catch (error) {
        showToast(error.message);
    } finally {
        approveButton.disabled = false;
        rejectButton.disabled = false;
    }
}

/**
 * 下载报告附件
 *
 * 该接口返回二进制文件，不使用 JSON 解析。
 */
async function downloadAttachment() {
    if (!currentSubmissionId) {
        showToast("报告信息不存在");
        return;
    }

    const button = document.getElementById("downloadAttachmentBtn");
    button.disabled = true;
    button.textContent = "正在下载...";

    try {
        const response = await fetch(
            `${API_BASE}/${currentSubmissionId}/attachment`,
            {
                method: "GET",
                credentials: "same-origin"
            }
        );

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "附件下载失败");
        }

        const blob = await response.blob();

        const disposition = response.headers.get("Content-Disposition") || "";
        const filenameMatch = disposition.match(/filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i);

        let filename = `submission-${currentSubmissionId}-attachment`;

        if (filenameMatch) {
            filename = decodeURIComponent(filenameMatch[1] || filenameMatch[2]);
        }

        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement("a");

        link.href = objectUrl;
        link.download = filename;

        document.body.appendChild(link);
        link.click();
        link.remove();

        URL.revokeObjectURL(objectUrl);

        showToast("附件下载成功");

    } catch (error) {
        showToast(error.message);
    } finally {
        button.disabled = false;
        button.textContent = "下载附件";
    }
}

/**
 * 关闭详情弹窗
 */
function closeModal() {
    elements.modal.classList.add("hidden");
    currentSubmissionId = null;
    currentSubmission = null;
}

/**
 * 状态中文名称
 */
function getStatusText(status) {
    const statusMap = {
        PENDING: "待审核",
        APPROVED: "已通过",
        REJECTED: "已驳回"
    };

    return statusMap[status] || "未知状态";
}

/**
 * 状态样式
 */
function getStatusClass(status) {
    const classMap = {
        PENDING: "status-pending",
        APPROVED: "status-approved",
        REJECTED: "status-rejected"
    };

    return classMap[status] || "";
}

/**
 * 格式化日期
 */
function formatDate(value) {
    if (!value) {
        return "-";
    }

    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;

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
 * 设置文本，避免把后端内容当作 HTML 执行
 */
function setText(id, value) {
    document.getElementById(id).textContent =
        value === null || value === undefined || value === ""
            ? "-"
            : String(value);
}

/**
 * HTML 转义
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
 * 显示错误信息
 */
function showError(message) {
    elements.errorMessage.textContent = message;
    elements.errorMessage.classList.remove("hidden");
}

/**
 * 显示操作提示
 */
function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.remove("hidden");

    window.setTimeout(() => {
        elements.toast.classList.add("hidden");
    }, 2500);
}
