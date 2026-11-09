package com.websecurity.platform.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ReviewRequest {

    @NotBlank(message = "审核状态不能为空")
    private String reviewStatus;

    @NotNull(message = "请填写实验成绩")
    @DecimalMin(value = "0.00", message = "成绩不能低于0分")
    @DecimalMax(value = "100.00", message = "成绩不能超过100分")
    private BigDecimal score;

    private String reviewComment;

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}