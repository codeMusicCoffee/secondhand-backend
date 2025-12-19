package com.example.secondhand.common;

import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    private Object user;

    public JwtResponse(String token, Object user) {
        this.token = token;
        this.user = user;
    }
}
