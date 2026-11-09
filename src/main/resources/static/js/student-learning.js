
/**
 * HTML 转义，防止接口数据被当作 HTML 执行
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
 * 任务状态中文名称
 */
const statusNames = {
    CLAIMED: "已领取",
    IN_PROGRESS: "进行中",
    SUBMITTED: "待审核",
    COMPLETED: "已完成"
};

/**
 * 格式化日期
 */
function formatDate(value) {
    if (!value) {
        return "-";
    }

    return String(value)
        .replace("T", " ")
        .substring(0, 19);
}

/**
 * 加载学习分析数据
 */
async function loadLearningOverview() {
    const container = document.getElementById("learningRecords");

    try {
        const response = await fetch(
            "/api/student/learning/overview",
            {
                method: "GET",
                credentials: "same-origin",
                headers: {
                    "Accept": "application/json"
                }
            }
        );

        if (response.status === 401 || response.status === 403) {
            window.location.href = "/login";
            return;
        }

        const result = await response.json();

        if (!response.ok || !result.success || !result.data) {
            throw new Error(
                result.message || "获取学习分析数据失败"
            );
        }

        const data = result.data;

        // 1. 基础统计
        document.getElementById("totalTasks").textContent =
            data.totalTasks ?? 0;

        document.getElementById("completedTasks").textContent =
            data.completedTasks ?? 0;

        document.getElementById("completionRate").textContent =
            `${data.completionRate ?? 0}%`;

        // 2. 平均成绩
        const scoredTaskCount = data.scoredTaskCount ?? 0;

        document.getElementById("averageScore").textContent =
            scoredTaskCount > 0
                ? Number(data.averageScore ?? 0).toFixed(2)
                : "--";

        document.getElementById("scoredTaskCount").textContent =
            scoredTaskCount > 0
                ? `根据 ${scoredTaskCount} 个任务的最近一次已审核成绩计算`
                : "暂无已评成绩";

        // 3. 学习进度
        const completionRate = Math.max(
            0,
            Math.min(100, Number(data.completionRate ?? 0))
        );

        document.getElementById("progressNumber").textContent =
            `${completionRate}%`;

        document.getElementById("progressBar").style.width =
            `${completionRate}%`;

        document.getElementById("claimedTasks").textContent =
            data.claimedTasks ?? 0;

        document.getElementById("inProgressTasks").textContent =
            data.inProgressTasks ?? 0;

        document.getElementById("pendingTasks").textContent =
            data.pendingTasks ?? 0;

        document.getElementById("completedStatusTasks").textContent =
            data.completedTasks ?? 0;

        // 4. 学习记录
        const records = data.records || [];

        if (records.length === 0) {
            container.innerHTML = `
                <tr>
                    <td colspan="6" class="empty">
                        暂无学习记录，快去领取一个实验任务吧。
                    </td>
                </tr>
            `;
            return;
        }

        container.innerHTML = records.map(item => {
            const status = item.status || "";
            const statusName = statusNames[status] || status;

            const score = item.score === null || item.score === undefined
                ? "-"
                : Number(item.score).toFixed(2);

            return `
                <tr>
                    <td>${escapeHtml(item.taskId)}</td>

                    <td>
                        ${escapeHtml(item.experimentName || "未知实验")}
                    </td>

                    <td>
                        ${escapeHtml(item.difficulty || "-")}
                    </td>

                    <td>
                        <span class="status-badge status-${escapeHtml(status)}">
                            ${escapeHtml(statusName)}
                        </span>
                    </td>

                    <td>
                        <span class="score-value">
                            ${escapeHtml(score)}
                        </span>
                    </td>

                    <td>
                        ${escapeHtml(formatDate(item.createdAt))}
                    </td>
                </tr>
            `;
        }).join("");

    } catch (error) {
        console.error("学习分析加载失败：", error);

        container.innerHTML = `
            <tr>
                <td colspan="6" class="empty error">
                    ${escapeHtml(error.message || "加载失败，请稍后重试")}
                </td>
            </tr>
        `;
    }
}

/**
 * 页面初始化
 */
document.addEventListener("DOMContentLoaded", function () {
    loadLearningOverview();
});
