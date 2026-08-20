package com.rongan.security_system.repository;

import com.rongan.security_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * User 的 Repository 接口，继承 JpaRepository 获得基本的 CRUD 方法
 */
public interface UserRepository extends JpaRepository<User, Long> {
    // 根据用户名查询用户 用于登录验证
    Optional<User> findByUsername(String username);
}
