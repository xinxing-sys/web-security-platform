
package com.websecurity.platform.controller;

import com.websecurity.platform.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminLabPageController {

    @GetMapping("/admin/labs")
    public String adminLabs(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            return "redirect:/login";
        }

        Object loginUser = session.getAttribute("loginUser");

        if (!(loginUser instanceof SysUser)) {
            return "redirect:/login";
        }

        SysUser user = (SysUser) loginUser;

        if (user.getId() == null
                || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        return "admin-labs";
    }
}
