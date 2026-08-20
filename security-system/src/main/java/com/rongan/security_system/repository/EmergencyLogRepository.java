package com.rongan.security_system.repository;

import com.rongan.security_system.entity.EmergencyLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 应急日志数据访问接口
 */
public interface EmergencyLogRepository extends JpaRepository<EmergencyLog, Long> {
}
