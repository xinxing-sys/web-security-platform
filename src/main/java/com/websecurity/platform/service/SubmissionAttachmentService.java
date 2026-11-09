package com.websecurity.platform.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface SubmissionAttachmentService {

    /**
     * 学生上传或替换自己的实验报告附件
     */
    String upload(Long userId, Long submissionId, MultipartFile file);

    /**
     * 学生下载自己的报告附件
     */
    Path getStudentAttachment(Long userId, Long submissionId);

    /**
     * 管理员下载任意报告附件
     */
    Path getAdminAttachment(Long submissionId);
}