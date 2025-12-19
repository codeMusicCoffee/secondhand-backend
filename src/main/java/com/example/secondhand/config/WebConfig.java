/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-10 20:11:26
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-10 20:50:06
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\config\WebConfig.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.beans.factory.InitializingBean;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer, InitializingBean {

    @Autowired
    private UploadConfig uploadConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            String uploadPath = uploadConfig.getActualUploadPath();
            Path path = Paths.get(uploadPath);
            
            System.out.println("=== 静态资源配置验证 ===");
            System.out.println("🔍 当前工作目录: " + System.getProperty("user.dir"));
            System.out.println("📁 配置的上传目录: " + uploadPath);
            System.out.println("📂 上传目录绝对路径: " + path.toAbsolutePath());
            System.out.println("✅ 目录是否存在: " + Files.exists(path));
            System.out.println("✅ 目录是否可读: " + Files.isReadable(path));
            System.out.println("✅ 目录是否可写: " + Files.isWritable(path));
            
            if (!Files.exists(path)) {
                System.out.println("⚠️  上传目录不存在，将在首次上传时自动创建");
            }
            
            // 验证静态资源映射URL格式
            String resourceUrl = "file:" + uploadPath;
            System.out.println("🔗 静态资源映射URL: /static/** -> " + resourceUrl);
            System.out.println("========================");
            
        } catch (Exception e) {
            System.err.println("❌ 静态资源配置验证失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // 使用配置类获取上传路径
            String uploadPath = uploadConfig.getActualUploadPath();
            String resourceUrl = "file:" + uploadPath;
            
            // 验证路径格式
            if (!uploadPath.endsWith(File.separator)) {
                uploadPath += File.separator;
                resourceUrl = "file:" + uploadPath;
            }
            
            // 配置静态资源映射
            registry.addResourceHandler("/static/**")
                    .addResourceLocations(resourceUrl)
                    .setCachePeriod(3600); // 缓存1小时
                    
            System.out.println("✅ 静态资源映射配置成功: /static/** -> " + resourceUrl);
            
        } catch (Exception e) {
            System.err.println("❌ 静态资源映射配置失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("静态资源配置失败", e);
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 配置CORS，允许前端访问
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:3000", 
                    "http://localhost:5173", 
                    "http://localhost:5174",  // 添加5174端口支持
                    "http://127.0.0.1:5173",
                    "http://127.0.0.1:5174"   // 添加127.0.0.1的5174端口支持
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}