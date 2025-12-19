package com.example.secondhand.controller;

import com.example.secondhand.config.UploadConfig;
import com.example.secondhand.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private UploadConfig uploadConfig;
    
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/upload-path")
    public Map<String, Object> getUploadPath() {
        Map<String, Object> result = new HashMap<>();
        
        String currentDir = System.getProperty("user.dir");
        String uploadDir = fileUploadService.getUploadDirectory();
        
        result.put("currentWorkingDirectory", currentDir);
        result.put("uploadDirectory", uploadDir);
        result.put("uploadDirectoryExists", new File(uploadDir).exists());
        result.put("configuredDir", uploadConfig.getDir());
        result.put("configuredPath", uploadConfig.getPath());
        
        return result;
    }
}