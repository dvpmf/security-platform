package com.rongan.security_system.controller;

import com.rongan.security_system.entity.User;
import com.rongan.security_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录控制器
 * 处理用户登录认证
 * 支持密码加密存储和明文兼容
 */
@RestController
@RequestMapping("/api/user")
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录接口
     * 验证用户名密码，支持加密和明文两种存储方式
     * 明文密码登录后自动升级为加密存储
     * @param loginUser 登录用户信息，包含用户名和密码
     * @return 登录结果，包含成功状态、提示消息和用户数据
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginUser) {
        Map<String, Object> result = new HashMap<>();
        // 根据用户名查询用户
        User user = userRepository.findByUsername(loginUser.getUsername()).orElse(null);
        
        if (user != null) {
            // 优先使用加密方式验证密码
            boolean passwordMatch = passwordEncoder.matches(loginUser.getPassword(), user.getPassword());
            
            // 加密验证失败，尝试明文比对，兼容历史数据
            if (!passwordMatch) {
                passwordMatch = loginUser.getPassword().equals(user.getPassword());
                
                // 明文验证成功，自动升级为加密存储
                if (passwordMatch) {
                    user.setPassword(passwordEncoder.encode(loginUser.getPassword()));
                    userRepository.save(user);
                    System.out.println("用户 " + user.getUsername() + " 的密码已自动升级为加密存储");
                }
            }
            
            // 组装登录结果
            if (passwordMatch) {
                result.put("success", true);
                result.put("message", "登录成功");
                result.put("data", user);
            } else {
                result.put("success", false);
                result.put("message", "用户名或密码错误");
            }
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }
}
