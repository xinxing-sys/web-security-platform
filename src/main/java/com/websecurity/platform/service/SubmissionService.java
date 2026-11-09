package com.websecurity.platform.service;

import com.websecurity.platform.entity.ExperimentSubmit;

import java.util.List;

public interface SubmissionService {

    /**
     * 学生提交实验报告
     */
    ExperimentSubmit submit(
            Long userId,
            Long taskId,
            String content
    );

    /**
     * 查询当前学生某个任务的全部报告历史
     */
    List<ExperimentSubmit> queryMySubmissions(
            Long userId,
            Long taskId
    );

    /**
     * 查询当前学生自己的报告详情
     */
    ExperimentSubmit getMySubmission(
            Long userId,
            Long submissionId
    );
}