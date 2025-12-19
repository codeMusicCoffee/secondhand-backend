/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-08 15:20:56
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-10 14:40:55
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\controller\UserController.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.controller;

import com.example.secondhand.common.JwtResponse;
import com.example.secondhand.common.JwtUtil;
import com.example.secondhand.common.Result;
import com.example.secondhand.entity.User;
import com.example.secondhand.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/user")
@CrossOrigin // 允许跨域
public class UserController {

    @Resource
    private UserRepository userRepository;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * ✅ 1. 用户登录（返回 token + 用户信息）
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody User user) {

        User dbUser = userRepository.findByUsername(user.getUsername());

        if (dbUser == null) {
            return Result.error("用户不存在");
        }

        if (!Objects.equals(dbUser.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }

        // 生成 Token，同时传递userId和username
        String token = JwtUtil.generateToken(dbUser.getId(), dbUser.getUsername());

        // 返回 token + user
        JwtResponse data = new JwtResponse(token, dbUser);
        return Result.success(data);
    }

    /**
     * ✅ 2. 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        // 检查用户名是否已存在
        User existByUsername = userRepository.findByUsername(user.getUsername());
        if (existByUsername != null) {
            return Result.error("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        User existByEmail = userRepository.findByEmail(user.getEmail());
        if (existByEmail != null) {
            return Result.error("邮箱已被注册");
        }
        
        // 保存用户
        User saved = userRepository.save(user);
        // 不返回密码信息
        saved.setPassword(null);
        return Result.success(saved);
    }

    /**
     * ✅ 3. 添加用户（管理员功能）
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody User user) {
        User exist = userRepository.findByUsername(user.getUsername());
        if (exist != null) {
            return Result.error("用户名已存在");
        }
        User saved = userRepository.save(user);
        return Result.success(saved);
    }

    /**
     * ✅ 3. 用户列表
     */
    @GetMapping("/list")
    public Result<List<User>> list() {
        return Result.success(userRepository.findAll());
    }

    /**
     * ✅ 4. 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userRepository.deleteById(id);
        return Result.success("删除成功");
    }
}
