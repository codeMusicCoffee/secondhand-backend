package com.example.secondhand.controller;

import com.example.secondhand.common.Result;
import com.example.secondhand.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"}, 
             allowedHeaders = "*", 
             methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class UploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/image")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 使用文件上传服务保存文件
            String fileUrl = fileUploadService.saveFile(file);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("originalName", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("type", file.getContentType());
            
            return Result.success(result);
            
        } catch (IllegalArgumentException e) {
            // 文件验证错误
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
}
