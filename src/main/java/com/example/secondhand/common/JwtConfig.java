package com.example.secondhand.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置类
 * 用于配置JWT相关属性
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT密钥
     */
    private String secret = "mySecretKey123456789012345678901234567890";

    /**
     * JWT过期时间（毫秒），默认24小时
     */
    private Long expiration = 86400000L;

    /**
     * JWT请求头名称
     */
    private String header = "Authorization";

    /**
     * JWT token前缀
     */
    private String tokenPrefix = "Bearer ";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }
}