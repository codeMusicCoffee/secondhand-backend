package com.example.secondhand.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.InitializingBean;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadConfig implements InitializingBean {
    
    private String dir = "uploads";
    private String path;
    
    public String getDir() {
        return dir;
    }
    
    public void setDir(String dir) {
        this.dir = dir;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * 获取实际的上传目录路径
     */
    public String getActualUploadPath() {
        String actualPath;
        
        if (path != null && !path.isEmpty()) {
            // 使用配置的绝对路径
            actualPath = path;
        } else {
            // 默认使用当前项目目录下的uploads文件夹
            String currentDir = System.getProperty("user.dir");
            actualPath = currentDir + File.separator + dir;
        }
        
        // 确保路径以分隔符结尾
        if (!actualPath.endsWith(File.separator)) {
            actualPath += File.separator;
        }
        
        return actualPath;
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean validateConfiguration() {
        try {
            String uploadPath = getActualUploadPath();
            Path path = Paths.get(uploadPath);
            
            // 检查父目录是否存在且可写
            Path parentPath = path.getParent();
            if (parentPath != null && Files.exists(parentPath)) {
                return Files.isWritable(parentPath);
            }
            
            // 如果目录已存在，检查是否可读写
            if (Files.exists(path)) {
                return Files.isReadable(path) && Files.isWritable(path);
            }
            
            // 目录不存在但父目录可写，认为配置有效
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 上传配置验证失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取环境适配的配置信息
     */
    public String getEnvironmentInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== 上传配置信息 ===\n");
        info.append("配置目录名: ").append(dir).append("\n");
        info.append("配置路径: ").append(path != null ? path : "未设置").append("\n");
        info.append("实际路径: ").append(getActualUploadPath()).append("\n");
        info.append("配置有效: ").append(validateConfiguration()).append("\n");
        info.append("==================");
        return info.toString();
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(getEnvironmentInfo());
    }
}