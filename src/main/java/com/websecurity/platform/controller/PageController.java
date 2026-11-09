
package com.websecurity.platform.controller;

import com.websecurity.platform.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * 学生工作台
     */
    @GetMapping("/student")
    public String student(HttpServletRequest request) {
        if (!isStudent(request)) {
            return "redirect:/login";
        }
        return "student";
    }

    /**
     * 学生实验详情页面
     */
    @GetMapping("/student/experiments/{id}")
    public String experimentDetail(
            @PathVariable Long id,
            HttpServletRequest request,
            Model model) {

        if (!isStudent(request)) {
            return "redirect:/login";
        }

        model.addAttribute("experimentId", id);
        return "experiment-detail";
    }


    /**
     * 学生实验报告页面
     */
    @GetMapping("/student/tasks/{taskId}/report")
    public String experimentReport(
            @PathVariable Long taskId,
            HttpServletRequest request,
            Model model) {

        if (!isStudent(request)) {
            return "redirect:/login";
        }

        model.addAttribute("taskId", taskId);
        return "report";
    }


    /**
     * 判断当前用户是否为已登录学生
     */
    private boolean isStudent(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        SysUser user = (SysUser) session.getAttribute("loginUser");

        return user != null
                && user.getId() != null
                && "STUDENT".equals(user.getRole());
    }
}
