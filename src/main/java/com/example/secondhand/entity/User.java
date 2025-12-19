/*
 * @Author: 'txy' '841067099@qq.com'
 * @Date: 2025-12-08 15:19:33
 * @LastEditors: 'txy' '841067099@qq.com'
 * @LastEditTime: 2025-12-11 11:17:21
 * @FilePath: \secondhand-try\backend\secondhand\src\main\java\com\example\secondhand\entity\User.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package com.example.secondhand.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户名
    @Column(unique = true, nullable = false)
    private String username;

    // 密码
    @Column(nullable = false)
    private String password;

    // 邮箱
@Column(unique = true)
private String email;

    // 创建时间
    private LocalDateTime createTime = LocalDateTime.now();
}
