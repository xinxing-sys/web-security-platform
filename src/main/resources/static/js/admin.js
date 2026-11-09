
/**
 * 管理员工作台
 */

// 请求接口
async function adminApiRequest(url) {
    const response = await fetch(url, {
        method: "GET",
        credentials: "same-origin"
    });

    if (response.status === 401 || response.status === 403) {
        window.location.href = "/login";
        throw new Error("登录状态已失效");
    }

    const result = await response.json();

    if (!response.ok || !result.success) {
        throw new Error(result.message || "接口请求失败");
    }

    return result.data;
}

// 加载管理员信息
async function loadAdminInfo() {
    const user = await adminApiRequest("/api/user/me");

    if (user.role !== "ADMIN") {
        window.location.href = "/login";
        return;
    }

    document.getElementById("adminName").textContent =
        user.nickname || user.username || "管理员";
}

// 加载平台数据统计
async function loadDashboardStatistics() {
    const requests = [
        {
            url: "/api/admin/users?pageNum=1&pageSize=1",
            elementId: "userCount",
            type: "page"
        },
        {
            url: "/api/admin/experiments?pageNum=1&pageSize=1",
            elementId: "experimentCount",
            type: "page"
        },
        {
            url: "/api/admin/vulnerabilities?pageNum=1&pageSize=1",
            elementId: "vulnerabilityCount",
            type: "page"
        },
        {
            url: "/api/admin/submissions?status=PENDING",
            elementId: "pendingCount",
            type: "list"
        }
    ];

    for (const item of requests) {
        try {
            const data = await adminApiRequest(item.url);

            let count = 0;

            if (item.type === "page") {
                count = data.total ?? 0;
            } else if (item.type === "list") {
                count = Array.isArray(data) ? data.length : 0;
            }

            document.getElementById(item.elementId).textContent =
                count;

        } catch (error) {
            console.error(
                `加载统计数据失败：${item.elementId}`,
                error
            );

            document.getElementById(item.elementId).textContent =
                "加载失败";
        }
    }
}

// 管理员退出登录
async function adminLogout() {
    const confirmed = window.confirm("确定退出管理员账号吗？");

    if (!confirmed) {
        return;
    }

    try {
        const response = await fetch("/api/user/logout", {
            method: "POST",
            credentials: "same-origin"
        });

        const result = await response.json();

        if (!response.ok || !result.success) {
            throw new Error(result.message || "退出登录失败");
        }

        window.location.href = "/login";

    } catch (error) {
        console.error("退出登录失败：", error);
        alert(error.message || "退出登录失败，请重试。");
    }
}

// 页面初始化
async function initAdminPage() {
    document.getElementById("logoutButton")
        .addEventListener("click", adminLogout);

    try {
        await loadAdminInfo();
        await loadDashboardStatistics();

    } catch (error) {
        console.error("管理员工作台初始化失败：", error);
    }
}

document.addEventListener("DOMContentLoaded", initAdminPage);
