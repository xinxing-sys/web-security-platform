package com.websecurity.platform.config;

import com.websecurity.platform.interceptor.LoginInterceptor;
import com.websecurity.platform.interceptor.OperationLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    private final OperationLogInterceptor operationLogInterceptor;

    public WebConfig(
            LoginInterceptor loginInterceptor,
            OperationLogInterceptor operationLogInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.operationLogInterceptor = operationLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 操作日志拦截器先执行，记录登录、退出等操作
        registry.addInterceptor(operationLogInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/logs/**")
                .order(0);

        // 原有登录拦截器保持原来的认证规则
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login",
                        "/api/user/logout",
                        "/api/test/db"
                )
                .order(1);
    }
}