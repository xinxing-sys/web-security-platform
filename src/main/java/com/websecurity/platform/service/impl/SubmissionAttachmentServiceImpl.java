package com.websecurity.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.websecurity.platform.entity.ExperimentSubmit;
import com.websecurity.platform.mapper.ExperimentSubmitMapper;
import com.websecurity.platform.service.SubmissionAttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SubmissionAttachmentServiceImpl
        implements SubmissionAttachmentService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "png", "jpg", "jpeg", "docx", "txt");

    private final ExperimentSubmitMapper submitMapper;

    @Value("${app.upload.storage-dir:uploads/submissions}")
    private String storageDir;

    public SubmissionAttachmentServiceImpl(
            ExperimentSubmitMapper submitMapper) {
        this.submitMapper = submitMapper;
    }

    /**
     * 上传或替换附件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(
            Long userId,
            Long submissionId,
            MultipartFile file) {

        if (userId == null || submissionId == null) {
            throw new IllegalArgumentException("用户或报告信息无效");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("附件大小不能超过10MB");
        }

        // 查询报告，并验证报告归属
        ExperimentSubmit submission = submitMapper.selectById(submissionId);

        if (submission == null
                || !userId.equals(submission.getUserId())) {
            throw new IllegalArgumentException("报告不存在或无权操作");
        }

        // 仅允许待审核报告上传或替换附件
        if (!"PENDING".equals(submission.getReviewStatus())) {
            throw new IllegalArgumentException(
                    "只有待审核状态的报告可以上传或替换附件");
        }

        String extension = getExtension(file.getOriginalFilename());

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "不支持该文件格式，仅允许 PDF、PNG、JPG、JPEG、DOCX、TXT");
        }

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取上传文件失败");
        }

        // 校验文件真实内容，不仅依赖扩展名
        validateFileContent(extension, bytes);

        Path root = getStorageRoot();
        String newFileName = UUID.randomUUID() + "." + extension;
        Path newFilePath = root.resolve(newFileName).normalize();

        String oldAttachmentPath = submission.getAttachmentPath();

        try {
            Files.createDirectories(root);
            Files.write(newFilePath, bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("保存附件失败");
        }

        // 仅更新当前学生自己的待审核报告
        int updateResult = submitMapper.update(
                null,
                new LambdaUpdateWrapper<ExperimentSubmit>()
                        .eq(ExperimentSubmit::getId, submissionId)
                        .eq(ExperimentSubmit::getUserId, userId)
                        .eq(ExperimentSubmit::getReviewStatus, "PENDING")
                        .set(ExperimentSubmit::getAttachmentPath, newFileName)
        );

        if (updateResult != 1) {
            deleteQuietly(newFilePath);
            throw new IllegalArgumentException(
                    "附件更新失败，报告状态可能已经发生变化");
        }

        // 数据库提交成功后删除旧附件；
        // 如果事务回滚，则删除刚刚上传的新附件。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            deleteStoredFileQuietly(oldAttachmentPath);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                deleteQuietly(newFilePath);
                            }
                        }
                    }
            );
        }

        return newFileName;
    }

    /**
     * 学生下载自己的附件
     */
    @Override
    public Path getStudentAttachment(
            Long userId,
            Long submissionId) {

        if (userId == null || submissionId == null) {
            throw new IllegalArgumentException("请求参数无效");
        }

        ExperimentSubmit submission = submitMapper.selectById(submissionId);

        if (submission == null
                || !userId.equals(submission.getUserId())) {
            throw new IllegalArgumentException("报告不存在或无权下载");
        }

        return resolveStoredFile(submission.getAttachmentPath());
    }

    /**
     * 管理员下载报告附件
     */
    @Override
    public Path getAdminAttachment(Long submissionId) {

        if (submissionId == null) {
            throw new IllegalArgumentException("报告ID不能为空");
        }

        ExperimentSubmit submission = submitMapper.selectById(submissionId);

        if (submission == null) {
            throw new IllegalArgumentException("实验报告不存在");
        }

        return resolveStoredFile(submission.getAttachmentPath());
    }

    /**
     * 获取附件存储根目录
     */
    private Path getStorageRoot() {

        return Paths.get(storageDir)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 从原始文件名中提取扩展名
     */
    private String getExtension(String originalFilename) {

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名无效");
        }

        String normalized = originalFilename.replace('\\', '/');

        // 只取最后一个文件名部分
        String fileName = normalized.substring(
                normalized.lastIndexOf('/') + 1
        );

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("文件必须包含有效扩展名");
        }

        return fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 根据文件扩展名检查真实文件内容
     */
    private void validateFileContent(
            String extension,
            byte[] bytes) {

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }

        boolean valid;

        switch (extension) {

            case "pdf":
                valid = startsWith(bytes, new byte[]{
                        0x25, 0x50, 0x44, 0x46, 0x2D
                });
                break;

            case "png":
                valid = startsWith(bytes, new byte[]{
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                });
                break;

            case "jpg":
            case "jpeg":
                valid = bytes.length >= 3
                        && (bytes[0] & 0xFF) == 0xFF
                        && (bytes[1] & 0xFF) == 0xD8
                        && (bytes[2] & 0xFF) == 0xFF;
                break;

            case "docx":
                valid = isValidDocx(bytes);
                break;

            case "txt":
                valid = isValidUtf8Text(bytes);
                break;

            default:
                valid = false;
        }

        if (!valid) {
            throw new IllegalArgumentException(
                    "文件内容与文件扩展名不匹配，上传已拒绝");
        }
    }

    /**
     * 检查文件头
     */
    private boolean startsWith(byte[] data, byte[] signature) {

        if (data.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查 DOCX 是否为包含 Word 文档主体的 ZIP 文件
     */
    private boolean isValidDocx(byte[] bytes) {

        if (bytes.length < 4
                || bytes[0] != 'P'
                || bytes[1] != 'K') {
            return false;
        }

        boolean hasDocument = false;

        try (ZipInputStream zipInputStream =
                     new ZipInputStream(new ByteArrayInputStream(bytes))) {

            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {

                if ("word/document.xml".equals(entry.getName())) {
                    hasDocument = true;
                    break;
                }
            }

        } catch (IOException e) {
            return false;
        }

        return hasDocument;
    }

    /**
     * 检查 TXT 是否为有效 UTF-8 文本
     */
    private boolean isValidUtf8Text(byte[] bytes) {

        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));

            return true;

        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * 根据数据库中的相对路径安全定位文件
     */
    private Path resolveStoredFile(String storedPath) {

        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("该报告尚未上传附件");
        }

        // 数据库只允许保存服务端生成的 UUID 文件名
        if (!storedPath.matches(
                "^[0-9a-fA-F-]{36}\\.(pdf|png|jpg|jpeg|docx|txt)$")) {
            throw new IllegalArgumentException("附件存储路径无效");
        }

        Path root = getStorageRoot();
        Path filePath = root.resolve(storedPath).normalize();

        if (!filePath.startsWith(root)) {
            throw new IllegalArgumentException("附件路径不合法");
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("附件文件不存在");
        }

        return filePath;
    }

    /**
     * 删除存储的旧附件
     */
    private void deleteStoredFileQuietly(String storedPath) {

        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        try {
            Path root = getStorageRoot();

            if (!storedPath.matches(
                    "^[0-9a-fA-F-]{36}\\.(pdf|png|jpg|jpeg|docx|txt)$")) {
                return;
            }

            Path filePath = root.resolve(storedPath).normalize();

            if (filePath.startsWith(root)) {
                deleteQuietly(filePath);
            }

        } catch (Exception ignored) {
            // 删除旧附件失败不影响已经提交的数据库事务
        }
    }

    /**
     * 安全删除文件
     */
    private void deleteQuietly(Path path) {

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 文件清理失败不影响主业务
        }
    }
}