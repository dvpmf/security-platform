package com.rongan.security_system.repository;

import com.rongan.security_system.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    // 根据状态查询告警 0 为未处理 1 为已处理
    List<Alert> findByStatus(Integer status);
    
    // 根据状态统计告警数量
    long countByStatus(int status);
    
    // 根据等级统计告警数量
    long countByLevel(int level);
    
    // 近 7 天每日告警数量 按日期分组
    @Query(value = "SELECT DATE(create_time) as date, COUNT(*) as count FROM alert WHERE create_time >= :startDate GROUP BY DATE(create_time) ORDER BY date ASC", nativeQuery = true)
    List<Object[]> countByDay(@Param("startDate") LocalDateTime startDate);
    
    // 根据告警类型统计数量
    long countByAlertType(String alertType);
    
    // 统计指定设备的告警数量 根据设备名称区分
    @Query(value = "SELECT d.name, COUNT(a.id) FROM alert a JOIN device d ON a.device_id = d.id WHERE d.name IN :deviceNames GROUP BY d.name", nativeQuery = true)
    List<Object[]> countByDeviceNames(@Param("deviceNames") List<String> deviceNames);
}
