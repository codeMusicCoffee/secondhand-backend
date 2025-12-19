/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-12 13:59:04
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-12 13:59:20
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\config\filterConfig.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.config;

import com.example.secondhand.filter.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*"); // 拦截所有请求
        registration.setOrder(1);
        return registration;
    }
}
