package com.websecurity.platform.dto;

import jakarta.validation.constraints.NotBlank;

public class SubmitRequest {

    @NotBlank(message = "实验报告内容不能为空")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}