package com.websecurity.platform.controller;

import com.websecurity.platform.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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

        HttpSession session = request.getSession(false);

        if (session == null) {
            return "redirect:/login";
        }

        SysUser user = (SysUser) session.getAttribute("loginUser");

        if (user == null || user.getId() == null) {
            return "redirect:/login";
        }

        if (!"STUDENT".equals(user.getRole())) {
            return "redirect:/login";
        }

        return "student";
    }
}