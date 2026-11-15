
/**
 * 当前实验及任务信息
 */
let currentExperiment = null;
let currentTask = null;
let currentLabSession = null;

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
 * 设置靶场入口
 */
function setLabLink(accessUrl) {
    const labLink = document.getElementById("labLink");

    if (!accessUrl) {
        labLink.hidden = true;
        labLink.removeAttribute("href");
        return;
    }

    try {
        const url = new URL(accessUrl);

        // 只允许本机DVWA地址
        if (
            !["http:", "https:"].includes(url.protocol) ||
            !["localhost", "127.0.0.1"].includes(url.hostname)
        ) {
            throw new Error("靶场地址不符合本机实验环境要求");
        }

        labLink.href = url.href;
        labLink.hidden = false;

    } catch (error) {
        labLink.hidden = true;
        labLink.removeAttribute("href");
        showMessage(error.message, true);
    }
}

/**
 * 渲染当前任务状态
 */
function renderTaskState() {
    const description = document.getElementById("taskDescription");

    setLabLink(null);

    if (!currentTask) {
        description.textContent =
            "领取实验任务后，即可开始实验并进入对应的靶场环境。";

        setButton("领取实验");
        return;
    }

    switch (currentTask.status) {
        case "CLAIMED":
            description.textContent =
                "你已经领取该实验，可以开始实验。";

            setButton("开始实验");
            break;

        case "IN_PROGRESS":
            description.textContent =
                "实验正在进行中。点击下方按钮启动或进入专属DVWA靶场。";

            setButton("启动 / 进入DVWA靶场");
            break;

        case "SUBMITTED":
            description.textContent =
                "实验报告已经提交，请等待教师审核。";

            setButton("等待教师审核", true);
            break;

        case "COMPLETED":
            description.textContent =
                "该实验任务已经完成。";

            setButton("任务已完成", true);
            break;

        default:
            description.textContent =
                "当前任务状态暂不支持操作。";

            setButton("暂不可操作", true);
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

        // 如果任务正在进行且实验类型为DVWA，
        // 尝试查询是否已经有可复用的靶场实例。
        if (
            currentTask &&
            currentTask.status === "IN_PROGRESS" &&
            currentExperiment.platformType === "DVWA"
        ) {
            await loadExistingLabSession();
        }

    } catch (error) {
        console.error("查询任务状态失败：", error);

        showMessage(error.message, true);
        setButton("暂时无法查询任务", true);
    }
}

/**
 * 查询已有的DVWA实例
 */
async function loadExistingLabSession() {
    try {
        currentLabSession = await apiRequest(
            `/api/labs/tasks/${encodeURIComponent(currentTask.id)}`
        );

        if (
            currentLabSession &&
            currentLabSession.status === "RUNNING"
        ) {
            setLabLink(currentLabSession.accessUrl);
        }

    } catch (error) {
        // 尚未创建实例时不显示错误，
        // 学生点击启动按钮后再创建。
        console.info("当前任务尚无可复用的DVWA实例：", error.message);
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

    setButton("正在开始实验...", true);

    try {
        currentTask = await apiRequest(
            `/api/tasks/${encodeURIComponent(currentTask.id)}/start`,
            {
                method: "PUT"
            }
        );

        showMessage("实验已经开始，可以启动DVWA靶场了！");
        renderTaskState();

    } catch (error) {
        showMessage(error.message, true);
        await loadMyTask();
    }
}

/**
 * 启动或复用DVWA实例
 */
async function startDvwaLab() {
    if (!currentTask || !currentTask.id) {
        showMessage("请先领取并开始实验", true);
        return;
    }

    if (currentTask.status !== "IN_PROGRESS") {
        showMessage("请先开始实验，再进入靶场", true);
        return;
    }

    if (currentExperiment.platformType !== "DVWA") {
        showMessage(
            "当前实验尚未配置为DVWA平台，请联系管理员检查实验配置。",
            true
        );
        return;
    }

    setButton("正在启动DVWA，请稍候...", true);
    setLabLink(null);

    try {
        currentLabSession = await apiRequest(
            `/api/labs/tasks/${encodeURIComponent(currentTask.id)}/start`,
            {
                method: "POST"
            }
        );

        if (
            !currentLabSession ||
            currentLabSession.status !== "RUNNING" ||
            !currentLabSession.accessUrl
        ) {
            throw new Error(
                "靶场启动请求已返回，但实例尚未处于可用状态"
            );
        }

        setLabLink(currentLabSession.accessUrl);

        showMessage(
            "DVWA靶场启动成功！点击“进入DVWA实验环境”即可开始实验。"
        );

        setButton("重新检查 / 进入DVWA靶场");

    } catch (error) {
        console.error("启动DVWA失败：", error);

        showMessage(error.message, true);
        setButton("重新启动DVWA靶场");
    }
}

/**
 * 领取、开始和启动靶场按钮事件
 */
document.getElementById("claimButton")
    .addEventListener("click", async function () {

        if (!currentTask) {
            await claimExperiment();
            return;
        }

        if (currentTask.status === "CLAIMED") {
            await startExperiment();
            return;
        }

        if (currentTask.status === "IN_PROGRESS") {
            await startDvwaLab();
        }
    });

/**
 * 页面初始化
 */
document.addEventListener("DOMContentLoaded", function () {
    loadExperimentDetail();
});
