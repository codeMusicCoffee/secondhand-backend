/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-10 14:29:36
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-17 11:41:48
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\config\SecurityConfig.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.config;

import com.example.secondhand.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.cors(cors -> {}); // 允许跨域

        http.authorizeHttpRequests(auth -> auth
                // 登录接口放行
                .requestMatchers("/user/login", "/user/add").permitAll()
                // 注册接口放行
                .requestMatchers("/user/register").permitAll()
        // 商品 GET 接口放行（列表 & 详情）
                .requestMatchers("/product/list", "/product/*").permitAll()
                
                // 评论相关接口放行（查看评论无需登录）
                .requestMatchers("/comment/list", "/comment/top-level", "/comment/replies", "/comment/stats").permitAll()

                // 静态资源放行（重要：图片访问）
                .requestMatchers("/static/**").permitAll()

                // 上传文件是否放行（按需）
                .requestMatchers("/upload/**").permitAll()
                
                // 测试接口放行
                .requestMatchers("/test/**").permitAll()

                // Swagger 放行（可选）
                .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                ).permitAll()
        // ✅✅✅【新增：支付宝支付接口放行】
                .requestMatchers("/api/pay/**").permitAll()

                // 其他接口需要登录
                .anyRequest().authenticated()
        );

        // 添加 JWT Token 过滤器
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
