package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 用户实体类
 * 对应数据库表 users
 * 存储系统用户信息，包含登录凭证和角色权限
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /**
     * 用户ID
     * 自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     * 登录账号，唯一标识
     */
    private String username;

    /**
     * 登录密码
     * BCrypt加密存储
     */
    private String password;

    /**
     * 手机号
     * 用于接收短信通知
     */
    private String phone;

    /**
     * 用户角色
     * admin为管理员，user为普通用户
     */
    private String role;
}
