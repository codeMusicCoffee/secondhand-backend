package com.example.secondhand.config;

import com.example.secondhand.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class StartupConfig implements CommandLineRunner {

    @Autowired
    private FileUploadService fileUploadService;
    
    @Autowired
    private UploadConfig uploadConfig;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 应用启动中...");
        System.out.println("=================================");
        
        // 验证上传配置
        validateUploadConfiguration();
        
        // 确保上传目录存在
        fileUploadService.ensureUploadDirectoryExists();
        
        // 验证静态资源访问
        validateStaticResourceAccess();
        
        System.out.println("=================================");
        System.out.println("✅ 应用启动完成！图片上传功能已就绪");
    }
    
    private void validateUploadConfiguration() {
        System.out.println("🔍 验证上传配置...");
        
        String uploadDir = fileUploadService.getUploadDirectory();
        Path uploadPath = Paths.get(uploadDir);
        
        System.out.println("📁 上传目录: " + uploadDir);
        System.out.println("📂 绝对路径: " + uploadPath.toAbsolutePath());
        
        // 验证配置有效性
        boolean configValid = uploadConfig.validateConfiguration();
        System.out.println("✅ 配置有效性: " + (configValid ? "有效" : "无效"));
        
        if (!configValid) {
            System.err.println("⚠️  上传配置可能存在问题，请检查目录权限");
        }
    }
    
    private void validateStaticResourceAccess() {
        System.out.println("🔍 验证静态资源访问...");
        
        String uploadDir = fileUploadService.getUploadDirectory();
        Path uploadPath = Paths.get(uploadDir);
        
        try {
            // 检查目录是否存在
            if (Files.exists(uploadPath)) {
                System.out.println("✅ 上传目录存在");
                System.out.println("✅ 目录可读: " + Files.isReadable(uploadPath));
                System.out.println("✅ 目录可写: " + Files.isWritable(uploadPath));
                
                // 列出现有文件
                File[] files = uploadPath.toFile().listFiles();
                if (files != null && files.length > 0) {
                    System.out.println("📄 现有文件数量: " + files.length);
                    for (int i = 0; i < Math.min(3, files.length); i++) {
                        File file = files[i];
                        String url = "/static/" + file.getName();
                        System.out.println("   - " + file.getName() + " -> " + url);
                    }
                    if (files.length > 3) {
                        System.out.println("   ... 还有 " + (files.length - 3) + " 个文件");
                    }
                } else {
                    System.out.println("📄 目录为空，等待文件上传");
                }
            } else {
                System.out.println("⚠️  上传目录不存在，将在首次上传时创建");
            }
            
            System.out.println("🔗 静态资源映射: /static/** -> file:" + uploadDir);
            
        } catch (Exception e) {
            System.err.println("❌ 静态资源验证失败: " + e.getMessage());
        }
    }
}