package com.websecurity.platform.controller;

import com.websecurity.platform.common.ApiResponse;
import com.websecurity.platform.entity.SysUser;
import com.websecurity.platform.service.SubmissionAttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@RestController
public class SubmissionAttachmentController {

    private final SubmissionAttachmentService attachmentService;

    public SubmissionAttachmentController(
            SubmissionAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 学生上传或替换实验报告附件
     *
     * POST /api/submissions/{id}/attachment
     */
    @PostMapping(
            value = "/api/submissions/{id}/attachment",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<String> upload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        try {
            SysUser student = getStudent(request);

            String fileName = attachmentService.upload(
                    student.getId(),
                    id,
                    file
            );

            return ApiResponse.success(
                    "附件上传成功",
                    fileName
            );

        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.error("附件上传失败");
        }
    }

    /**
     * 学生下载自己的实验报告附件
     *
     * GET /api/submissions/{id}/attachment
     */
    @GetMapping("/api/submissions/{id}/attachment")
    public ResponseEntity<?> downloadByStudent(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            SysUser student = getStudent(request);

            Path filePath = attachmentService.getStudentAttachment(
                    student.getId(),
                    id
            );

            return buildDownloadResponse(filePath, id);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("附件下载失败"));
        }
    }

    /**
     * 管理员下载实验报告附件
     *
     * GET /api/admin/submissions/{id}/attachment
     */
    @GetMapping("/api/admin/submissions/{id}/attachment")
    public ResponseEntity<?> downloadByAdmin(
            @PathVariable Long id,
            HttpServletRequest request) {

        try {
            getAdmin(request);

            Path filePath = attachmentService.getAdminAttachment(id);

            return buildDownloadResponse(filePath, id);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("附件下载失败"));
        }
    }

    /**
     * 构建安全的文件下载响应
     */
    private ResponseEntity<Resource> buildDownloadResponse(
            Path filePath,
            Long submissionId) throws IOException {

        String extension = getExtension(filePath);

        String downloadFileName =
                "experiment-report-" + submissionId + "." + extension;

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                downloadFileName,
                                StandardCharsets.UTF_8
                        )
                        .build();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentDisposition(contentDisposition);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(Files.size(filePath));
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl("no-store");

        Resource resource = new FileSystemResource(filePath);

        return new ResponseEntity<>(
                resource,
                headers,
                HttpStatus.OK
        );
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(Path filePath) {

        String fileName = filePath.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("附件文件格式无效");
        }

        return fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 获取当前登录学生
     */
    private SysUser getStudent(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new IllegalArgumentException("请先登录");
        }

        Object userObject = session.getAttribute("loginUser");

        if (!(userObject instanceof SysUser)) {
            throw new IllegalArgumentException("登录状态无效，请重新登录");
        }

        SysUser user = (SysUser) userObject;

        if (user.getId() == null
                || !"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException("只有学生可以上传或下载自己的附件");
        }

        return user;
    }

    /**
     * 获取当前登录管理员
     */
    private SysUser getAdmin(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new IllegalArgumentException("请先登录");
        }

        Object userObject = session.getAttribute("loginUser");

        if (!(userObject instanceof SysUser)) {
            throw new IllegalArgumentException("登录状态无效，请重新登录");
        }

        SysUser user = (SysUser) userObject;

        if (user.getId() == null
                || !"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("只有管理员可以下载附件");
        }

        return user;
    }
}