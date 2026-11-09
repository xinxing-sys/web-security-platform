
/**
 * 学生实验报告页面
 *
 * 功能：
 * 1. 加载任务和实验信息
 * 2. 加载历史提交记录
 * 3. 填写并提交实验报告
 * 4. 上传和替换实验报告附件
 * 5. 下载实验报告附件
 */

const reportTaskId = window.taskId;

let currentTask = null;

/**
 * 任务状态映射
 */
const taskStatusMap = {
    CLAIMED: "已领取",
    IN_PROGRESS: "进行中",
    SUBMITTED: "已提交，等待审核",
    COMPLETED: "已完成"
};

/**
 * 报告审核状态映射
 */
const reviewStatusMap = {
    PENDING: "待审核",
    APPROVED: "审核通过",
    REJECTED: "审核未通过"
};

/**
 * HTML 转义，防止 XSS
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
 * 格式化时间
 */
function formatDate(value) {
    if (!value) {
        return "暂无";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return escapeHtml(value);
    }

    return date.toLocaleString("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });
}

/**
 * 统一请求接口
 */
async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options
    });

    if (response.status === 401 || response.status === 403) {
        window.location.href = "/login";
        throw new Error("登录状态已失效，请重新登录");
    }

    let result;

    try {
        result = await response.json();
    } catch {
        throw new Error("服务器返回的数据格式异常");
    }

    if (!response.ok || result.success !== true) {
        throw new Error(result.message || "请求失败");
    }

    return result.data;
}

/**
 * 显示任务状态及报告填写区域
 */
function renderTaskStatus() {
    const statusText =
        taskStatusMap[currentTask.status] ||
        currentTask.status ||
        "未知状态";

    document.getElementById("taskStatus").textContent =
        `任务状态：${statusText}`;

    const form = document.getElementById("reportForm");
    const tip = document.getElementById("reportFormTip");

    if (currentTask.status === "IN_PROGRESS") {
        form.style.display = "block";
        tip.textContent =
            "请认真填写实验报告，提交后将进入教师审核流程。";
    } else {
        form.style.display = "none";

        if (currentTask.status === "CLAIMED") {
            tip.textContent =
                "请先前往实验详情页面启动实验，再提交实验报告。";
        } else if (currentTask.status === "SUBMITTED") {
            tip.textContent =
                "报告已提交，请等待教师审核。";
        } else if (currentTask.status === "COMPLETED") {
            tip.textContent =
                "该实验任务已经完成。";
        } else {
            tip.textContent =
                "当前任务状态不允许提交报告。";
        }
    }
}

/**
 * 加载任务和实验信息
 */
async function loadTaskAndExperiment() {
    currentTask = await apiRequest(
        `/api/tasks/${reportTaskId}`
    );

    if (!currentTask || !currentTask.experimentId) {
        throw new Error("任务信息异常，未找到关联的实验");
    }

    const experiment = await apiRequest(
        `/api/experiments/${currentTask.experimentId}`
    );

    document.getElementById("experimentName").textContent =
        experiment.name || "未命名实验";

    renderTaskStatus();
}

/**
 * 获取审核状态文字
 */
function getReviewStatusText(status) {
    return reviewStatusMap[status] || status || "未知状态";
}

/**
 * 渲染历史报告
 */
function renderSubmissionHistory(submissions) {
    const container = document.getElementById("submissionHistory");

    if (!Array.isArray(submissions) || submissions.length === 0) {
        container.innerHTML = `
            <div class="empty-history">
                暂无提交记录。完成实验后，可以在这里查看报告提交情况。
            </div>
        `;
        return;
    }

    container.innerHTML = submissions.map((item, index) => {
        const statusText = getReviewStatusText(item.reviewStatus);

        const scoreText =
            item.score === null || item.score === undefined
                ? "暂未评分"
                : `${escapeHtml(item.score)} 分`;

        // 已上传附件时显示下载链接
        const attachmentHtml = item.attachmentPath
            ? `
                <a
                    class="attachment-download"
                    href="/api/submissions/${encodeURIComponent(item.id)}/attachment"
                >
                    下载报告附件
                </a>
            `
            : `
                <span class="no-attachment">
                    未上传附件
                </span>
            `;

        // 只有待审核报告允许上传或替换附件
        const uploadHtml = item.reviewStatus === "PENDING"
            ? `
                <div class="attachment-upload-area">
                    <input
                        type="file"
                        class="attachment-input"
                        data-submission-id="${escapeHtml(item.id)}"
                        accept=".pdf,.png,.jpg,.jpeg,.docx,.txt"
                    >

                    <button
                        type="button"
                        class="attachment-upload-button"
                        data-submission-id="${escapeHtml(item.id)}"
                    >
                        ${item.attachmentPath ? "替换附件" : "上传附件"}
                    </button>
                </div>
            `
            : "";

        return `
            <article class="history-item">

                <div class="history-header">
                    <strong>第 ${index + 1} 次提交</strong>

                    <span class="history-status">
                        ${escapeHtml(statusText)}
                    </span>
                </div>

                <p>
                    <strong>提交时间：</strong>
                    ${formatDate(item.submittedAt)}
                </p>

                <p>
                    <strong>成绩：</strong>
                    ${scoreText}
                </p>

                <p>
                    <strong>报告内容：</strong>
                </p>

                <div class="history-content">
                    ${escapeHtml(item.content || "暂无报告内容")}
                </div>

                <p>
                    <strong>教师评语：</strong>
                    ${escapeHtml(item.reviewComment || "暂无评语")}
                </p>

                <div class="history-attachment">
                    ${attachmentHtml}
                    ${uploadHtml}
                </div>

            </article>
        `;
    }).join("");
}

/**
 * 加载历史提交记录
 */
async function loadSubmissionHistory() {
    const submissions = await apiRequest(
        `/api/tasks/${reportTaskId}/submissions`
    );

    renderSubmissionHistory(submissions);
}

/**
 * 上传实验报告附件
 */
async function uploadSubmissionAttachment(
    submissionId,
    file,
    button
) {
    if (!file) {
        alert("请先选择需要上传的附件。");
        return;
    }

    // 允许的附件格式
    const allowedExtensions = [
        "pdf",
        "png",
        "jpg",
        "jpeg",
        "docx",
        "txt"
    ];

    const extension = file.name
        .split(".")
        .pop()
        .toLowerCase();

    if (!allowedExtensions.includes(extension)) {
        alert(
            "文件格式不正确，仅支持 PDF、PNG、JPG、JPEG、DOCX、TXT。"
        );
        return;
    }

    // 最大 10 MB
    if (file.size > 10 * 1024 * 1024) {
        alert("附件大小不能超过 10 MB。");
        return;
    }

    const formData = new FormData();

    // 后端 MultipartFile 参数字段名称为 file
    formData.append("file", file);

    const originalText = button.textContent;

    button.disabled = true;
    button.textContent = "正在上传...";

    try {
        await apiRequest(
            `/api/submissions/${submissionId}/attachment`,
            {
                method: "POST",
                body: formData
            }
        );

        alert("附件上传成功！");

        // 重新加载历史记录
        await loadSubmissionHistory();

    } catch (error) {
        console.error("附件上传失败：", error);

        alert(
            error.message || "附件上传失败，请稍后重试。"
        );

    } finally {
        button.disabled = false;
        button.textContent = originalText;
    }
}

/**
 * 显示提交结果提示
 */
function showSubmitMessage(message, isError = false) {
    const element = document.getElementById("submitMessage");

    element.textContent = message;

    element.className = isError
        ? "submit-message error"
        : "submit-message success";
}

/**
 * 提交实验报告
 */
async function submitReport() {
    const contentElement =
        document.getElementById("reportContent");

    const button =
        document.getElementById("submitReportButton");

    const content = contentElement.value.trim();

    if (!content) {
        showSubmitMessage("请先填写实验报告内容。", true);
        contentElement.focus();
        return;
    }

    if (!currentTask || currentTask.status !== "IN_PROGRESS") {
        showSubmitMessage(
            "当前任务状态不允许提交报告。",
            true
        );
        return;
    }

    const confirmed = window.confirm(
        "确定提交这份实验报告吗？"
    );

    if (!confirmed) {
        return;
    }

    button.disabled = true;
    button.textContent = "正在提交...";

    try {
        await apiRequest(
            `/api/tasks/${reportTaskId}/submit`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    content: content
                })
            }
        );

        showSubmitMessage(
            "实验报告提交成功，已进入教师审核流程。"
        );

        contentElement.value = "";

        // 更新本地任务状态
        currentTask.status = "SUBMITTED";

        renderTaskStatus();

        // 重新加载历史报告
        await loadSubmissionHistory();

    } catch (error) {
        console.error("提交实验报告失败：", error);

        showSubmitMessage(
            error.message || "提交失败，请稍后重试。",
            true
        );

    } finally {
        button.disabled = false;
        button.textContent = "提交实验报告";
    }
}

/**
 * 绑定附件上传事件
 *
 * 使用事件委托，因为历史报告内容是动态生成的。
 */
function bindAttachmentUploadEvents() {
    const container =
        document.getElementById("submissionHistory");

    container.addEventListener("click", async function(event) {
        const button = event.target.closest(
            ".attachment-upload-button"
        );

        if (!button || !container.contains(button)) {
            return;
        }

        const submissionId = button.dataset.submissionId;

        const fileInput = container.querySelector(
            `.attachment-input[data-submission-id="${submissionId}"]`
        );

        if (
            !fileInput ||
            !fileInput.files ||
            fileInput.files.length === 0
        ) {
            alert("请先选择需要上传的附件。");
            return;
        }

        await uploadSubmissionAttachment(
            submissionId,
            fileInput.files[0],
            button
        );
    });
}

/**
 * 页面初始化
 */
async function initReportPage() {
    if (
        !reportTaskId ||
        !Number.isInteger(Number(reportTaskId)) ||
        Number(reportTaskId) <= 0
    ) {
        document.getElementById("experimentName").textContent =
            "任务编号无效";

        document.getElementById("taskStatus").textContent =
            "无法加载任务信息";

        document.getElementById("submissionHistory").innerHTML = `
            <div class="empty-history">
                无效的任务编号，无法加载报告记录。
            </div>
        `;

        return;
    }

    // 绑定报告提交按钮
    document.getElementById("submitReportButton")
        .addEventListener("click", submitReport);

    // 绑定附件上传按钮事件
    bindAttachmentUploadEvents();

    try {
        await loadTaskAndExperiment();

        await loadSubmissionHistory();

    } catch (error) {
        console.error("加载实验报告页面失败：", error);

        document.getElementById("experimentName").textContent =
            "实验信息加载失败";

        document.getElementById("taskStatus").textContent =
            error.message || "任务信息加载失败";

        document.getElementById("submissionHistory").innerHTML = `
            <div class="empty-history">
                历史记录加载失败：
                ${escapeHtml(error.message)}
            </div>
        `;
    }
}

// 页面加载完成后初始化
document.addEventListener(
    "DOMContentLoaded",
    initReportPage
);
