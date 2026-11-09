package com.websecurity.platform.service;

import com.websecurity.platform.entity.ExperimentSubmit;

import java.util.List;

public interface AdminSubmissionService {

    /**
     * 查询全部实验报告，可按审核状态筛选
     */
    List<ExperimentSubmit> querySubmissions(String reviewStatus);

    /**
     * 查询报告详情
     */
    ExperimentSubmit getSubmission(Long submissionId);

    /**
     * 审核实验报告
     */
    ExperimentSubmit review(
            Long reviewerId,
            Long submissionId,
            String reviewStatus,
            java.math.BigDecimal score,
            String reviewComment
    );
}