package com.example.secondhand.service;

import com.example.secondhand.config.UploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
public class FileUploadService {

    @Autowired
    private UploadConfig uploadConfig;

    // 允许的文件类型
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    // 最大文件大小 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * 获取上传目录的绝对路径
     */
    public String getUploadDirectory() {
        String uploadPath = uploadConfig.getActualUploadPath();
        
        System.out.println("🔍 当前工作目录: " + System.getProperty("user.dir"));
        System.out.println("📁 配置的上传目录: " + uploadPath);
        
        return uploadPath;
    }

    /**
     * 确保上传目录存在
     */
    public void ensureUploadDirectoryExists() throws IOException {
        Path uploadPath = Paths.get(getUploadDirectory());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("✅ 创建上传目录: " + uploadPath.toAbsolutePath());
        }
    }

    /**
     * 验证上传的文件
     */
    public void validateFile(MultipartFile file) throws IllegalArgumentException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 JPG、PNG、GIF、WebP 格式");
        }
        
        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 5MB");
        }
    }

    /**
     * 生成唯一的文件名
     */
    public String generateFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return System.currentTimeMillis() + "_" + System.nanoTime() + extension;
    }

    /**
     * 保存文件到上传目录
     */
    public String saveFile(MultipartFile file) throws IOException {
        // 验证文件
        validateFile(file);
        
        // 确保目录存在
        ensureUploadDirectoryExists();
        
        // 生成文件名
        String fileName = generateFileName(file.getOriginalFilename());
        
        // 构建完整路径
        String uploadDir = getUploadDirectory();
        File destFile = new File(uploadDir + fileName);
        
        // 保存文件
        file.transferTo(destFile);
        
        // 返回访问URL
        String fileUrl = "/static/" + fileName;
        
        // 验证保存后的文件和URL
        validateSavedFile(destFile, fileUrl);
        
        System.out.println("✅ 文件保存成功:");
        System.out.println("   原文件名: " + file.getOriginalFilename());
        System.out.println("   新文件名: " + fileName);
        System.out.println("   保存路径: " + destFile.getAbsolutePath());
        System.out.println("   访问URL: " + fileUrl);
        System.out.println("   文件大小: " + destFile.length() + " bytes");
        System.out.println("   文件存在: " + destFile.exists());
        
        return fileUrl;
    }
    
    /**
     * 验证保存后的文件和URL有效性
     */
    private void validateSavedFile(File savedFile, String fileUrl) throws IOException {
        // 验证文件是否成功保存
        if (!savedFile.exists()) {
            throw new IOException("文件保存失败：文件不存在于 " + savedFile.getAbsolutePath());
        }
        
        // 验证文件大小
        if (savedFile.length() == 0) {
            throw new IOException("文件保存失败：文件大小为0");
        }
        
        // 验证文件可读性
        if (!savedFile.canRead()) {
            throw new IOException("文件保存失败：文件不可读");
        }
        
        // 验证URL格式
        if (!fileUrl.startsWith("/static/")) {
            throw new IOException("URL格式错误：" + fileUrl);
        }
        
        System.out.println("✅ 文件验证通过: " + savedFile.getAbsolutePath());
    }
}