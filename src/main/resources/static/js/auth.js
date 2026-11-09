/**
 * 显示页面提示信息
 */
function showMessage(message, type) {
    const messageElement = document.getElementById("message");

    if (!messageElement) {
        return;
    }

    messageElement.textContent = message;
    messageElement.className = "message " + type;
}

/**
 * 设置按钮状态
 */
function setButtonLoading(button, loading, normalText) {
    if (!button) {
        return;
    }

    button.disabled = loading;
    button.textContent = loading ? "请稍候..." : normalText;
}

/**
 * 用户登录
 */
const loginForm = document.getElementById("loginForm");

if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;
        const button = document.getElementById("loginButton");

        showMessage("", "");
        setButtonLoading(button, true, "登录");

        try {
            const response = await fetch("/api/user/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "same-origin",
                body: JSON.stringify({
                    username: username,
                    password: password
                })
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                showMessage(
                    result.message || "登录失败，请检查用户名和密码",
                    "error"
                );
                return;
            }

            const user = result.data;

            showMessage(
                "登录成功！欢迎回来，" +
                (user.nickname || user.username) +
                "。账号身份：" +
                (user.role === "ADMIN" ? "管理员" : "学生") +
                "。",
                "success"
            );

            // 登录接口已经创建 Session。
            // 工作台页面将在后续步骤中接入。

        } catch (error) {
            showMessage("网络异常，无法连接服务器，请检查平台是否正常运行。", "error");
        } finally {
            setButtonLoading(button, false, "登录");
        }
    });
}

/**
 * 用户注册
 */
const registerForm = document.getElementById("registerForm");

if (registerForm) {
    registerForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        const username = document.getElementById("username").value.trim();
        const password = document.getElementById("password").value;
        const nickname = document.getElementById("nickname").value.trim();
        const email = document.getElementById("email").value.trim();

        const button = document.getElementById("registerButton");

        showMessage("", "");
        setButtonLoading(button, true, "注册账号");

        try {
            const response = await fetch("/api/user/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "same-origin",
                body: JSON.stringify({
                    username: username,
                    password: password,
                    nickname: nickname,
                    email: email || null
                })
            });

            const result = await response.json();

            if (!response.ok || !result.success) {
                showMessage(
                    result.message || "注册失败，请检查填写的信息",
                    "error"
                );
                return;
            }

            showMessage(
                "注册成功！即将跳转到登录页面。",
                "success"
            );

            registerForm.reset();

            setTimeout(function () {
                window.location.href = "/login";
            }, 1200);

        } catch (error) {
            showMessage("网络异常，无法连接服务器，请检查平台是否正常运行。", "error");
        } finally {
            setButtonLoading(button, false, "注册账号");
        }
    });
}