
/**
 * 当前实验及任务信息
 */
let currentExperiment = null;
let currentTask = null;

/**
 * HTML 转义
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

    if (response.status === 401) {
        window.location.href = "/login";
        throw new Error("登录状态已失效");
    }

    const result = await response.json();

    if (!response.ok || !result.success) {
        throw new Error(result.message || "请求失败");
    }

    return result.data;
}

/**
 * 显示提示信息
 */
function showMessage(message, isError = false) {
    const element = document.getElementById("message");

    element.textContent = message;
    element.style.color = isError ? "#dc2626" : "#15803d";
}

/**
 * 设置按钮状态
 */
function setButton(text, disabled = false) {
    const button = document.getElementById("claimButton");

    button.textContent = text;
    button.disabled = disabled;
}

/**
 * 渲染当前任务状态
 */
function renderTaskState() {
    const description = document.getElementById("taskDescription");
    const labLink = document.getElementById("labLink");

    labLink.hidden = true;
    labLink.removeAttribute("href");

    if (!currentTask) {
        description.textContent =
            "领取实验任务后，即可开始实验并进入对应的靶场环境。";

        setButton("领取实验");
        return;
    }

    switch (currentTask.status) {
        case "CLAIMED":
            description.textContent = "你已经领取该实验，可以开始实验。";
            setButton("开始实验");
            break;

        case "IN_PROGRESS":
            description.textContent = "实验正在进行中，可以进入靶场继续练习。";
            setButton("实验进行中", true);
            showLabLink();
            break;

        case "SUBMITTED":
            description.textContent = "实验报告已经提交，请等待教师审核。";
            setButton("等待教师审核", true);
            break;

        case "COMPLETED":
            description.textContent = "该实验任务已经完成。";
            setButton("任务已完成", true);
            break;

        default:
            description.textContent = "当前任务状态暂不支持操作。";
            setButton("暂不可操作", true);
    }
}

/**
 * 解析靶场地址
 *
 * 只允许本机 WebGoat 地址，避免跳转到未知外部网站。
 */
function getLabUrl() {
    const configuredUrl = currentExperiment?.platformUrl;
    const lessonCode = currentExperiment?.lessonCode;

    // 优先使用实验配置的完整地址
    if (configuredUrl && configuredUrl.trim()) {
        const url = new URL(configuredUrl.trim());

        if (
            !["http:", "https:"].includes(url.protocol) ||
            !["localhost", "127.0.0.1"].includes(url.hostname)
        ) {
            throw new Error("实验靶场地址不符合本机实验环境要求");
        }

        return url.href;
    }

    // 没有配置完整地址时，根据 WebGoat 课程编号生成入口
    if (
        currentExperiment?.platformType === "WEBGOAT" &&
        lessonCode &&
        /^[a-zA-Z0-9_-]+\.lesson$/.test(lessonCode)
    ) {
        return `http://localhost:8081/WebGoat/start.mvc?username=student01#lesson/${lessonCode}`;
    }

    throw new Error("实验尚未配置有效的靶场入口或课程编号");
}

/**
 * 显示进入靶场入口
 */
function showLabLink() {
    try {
        const labLink = document.getElementById("labLink");

        labLink.href = getLabUrl();
        labLink.hidden = false;

    } catch (error) {
        showMessage(error.message, true);
    }
}

/**
 * 加载实验详情
 */
async function loadExperimentDetail() {
    const message = document.getElementById("message");

    try {
        currentExperiment = await apiRequest(
            `/api/experiments/${encodeURIComponent(experimentId)}`
        );

        document.getElementById("experimentName").textContent =
            currentExperiment.name || "未命名实验";

        document.getElementById("experimentDescription").textContent =
            currentExperiment.description || "暂无实验介绍";

        document.getElementById("experimentDifficulty").textContent =
            `难度：${currentExperiment.difficulty || "未设置"}`;

        document.getElementById("experimentPlatform").textContent =
            `实验平台：${currentExperiment.platformType || "未设置"}`;

        document.getElementById("experimentId").textContent =
            `实验编号：${currentExperiment.id}`;

        document.getElementById("experimentTarget").textContent =
            currentExperiment.target || "暂无实验目标";

        document.getElementById("experimentSteps").textContent =
            currentExperiment.steps || "暂无实验步骤";

        await loadMyTask();

    } catch (error) {
        console.error("加载实验详情失败：", error);

        message.textContent = error.message;

        document.getElementById("experimentName").textContent =
            "实验信息加载失败";

        document.getElementById("experimentDescription").textContent =
            "请返回学生工作台后重试。";

        setButton("暂不可操作", true);
    }
}

/**
 * 查询当前学生是否已经领取该实验
 */
async function loadMyTask() {
    try {
        const data = await apiRequest(
            "/api/tasks?pageNum=1&pageSize=100"
        );

        const records = data.records || [];

        currentTask = records.find(item =>
            Number(item.experimentId) === Number(experimentId)
        ) || null;

        renderTaskState();

    } catch (error) {
        console.error("查询任务状态失败：", error);
        showMessage(error.message, true);
        setButton("暂时无法查询任务", true);
    }
}

/**
 * 领取实验
 */
async function claimExperiment() {
    const button = document.getElementById("claimButton");

    button.disabled = true;
    button.textContent = "正在领取...";

    try {
        currentTask = await apiRequest(
            `/api/tasks/${encodeURIComponent(experimentId)}/claim`,
            {
                method: "POST"
            }
        );

        if (!currentTask || !currentTask.id) {
            throw new Error("领取成功，但没有获取到任务编号");
        }

        showMessage("实验任务领取成功！");
        renderTaskState();

    } catch (error) {
        showMessage(error.message, true);
        await loadMyTask();
    }
}

/**
 * 开始实验
 */
async function startExperiment() {
    if (!currentTask || !currentTask.id) {
        showMessage("请先领取实验任务", true);
        return;
    }

    setButton("正在启动...", true);

    try {
        currentTask = await apiRequest(
            `/api/tasks/${encodeURIComponent(currentTask.id)}/start`,
            {
                method: "PUT"
            }
        );

        showMessage("实验已经开始，可以进入靶场了！");
        renderTaskState();

    } catch (error) {
        showMessage(error.message, true);
        await loadMyTask();
    }
}

/**
 * 领取 / 开始按钮事件
 */
document.getElementById("claimButton")
    .addEventListener("click", async function () {

        if (!currentTask) {
            await claimExperiment();
            return;
        }

        if (currentTask.status === "CLAIMED") {
            await startExperiment();
        }
    });

/**
 * 页面初始化
 */
document.addEventListener("DOMContentLoaded", function () {
    loadExperimentDetail();
});
