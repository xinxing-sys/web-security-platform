
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
     * 管理员工作台
     */
    @GetMapping("/admin")
    public String admin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return "redirect:/login";
        }
        return "admin";
    }

    /**
     * 管理员实验报告审核页面
     */
    @GetMapping("/admin/submissions")
    public String adminSubmissions(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return "redirect:/login";
        }
        return "admin-submissions";
    }

    /**
     * 管理员用户管理页面
     */
    @GetMapping("/admin/users")
    public String adminUsers(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return "redirect:/login";
        }
        return "admin-users";
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
     * 学生漏洞知识详情页面
     */
    @GetMapping("/student/knowledge/{id}")
    public String vulnerabilityDetail(
            @PathVariable Long id,
            HttpServletRequest request,
            Model model) {

        if (!isStudent(request)) {
            return "redirect:/login";
        }

        model.addAttribute("vulnerabilityId", id);
        return "vulnerability-detail";
    }



    /**
     * 学生学习分析页面
     */
    @GetMapping("/student/learning")
    public String studentLearning(HttpServletRequest request) {
        if (!isStudent(request)) {
            return "redirect:/login";
        }
        return "student-learning";
    }





    /**
     * 管理员实验资源管理页面
     */
    @GetMapping("/admin/experiments")
    public String adminExperiments(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return "redirect:/login";
        }
        return "admin-experiments";
    }


    /**
     * 管理员操作日志页面
     */
    @GetMapping("/admin/logs")
    public String adminLogs(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return "redirect:/login";
        }
        return "admin-logs";
    }

    /**
     * 管理员漏洞知识与分类管理页面
     */
    @GetMapping("/admin/knowledge")
    public String adminKnowledge(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return "redirect:/login";
        }
        return "admin-knowledge";
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

    /**
     * 判断当前用户是否为已登录管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return false;
        }

        SysUser user = (SysUser) session.getAttribute("loginUser");

        return user != null
                && user.getId() != null
                && "ADMIN".equals(user.getRole());
    }
}
