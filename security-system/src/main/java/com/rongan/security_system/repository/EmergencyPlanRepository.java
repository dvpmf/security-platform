package com.rongan.security_system.repository;

import com.rongan.security_system.entity.EmergencyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 应急预案数据访问接口
 */
public interface EmergencyPlanRepository extends JpaRepository<EmergencyPlan, Long> {
    
    /**
     * 根据告警类型精确查询应急预案
     * @param alertType 告警类型
     * @return 匹配的应急预案，未找到返回 null
     */
    EmergencyPlan findByAlertType(String alertType);
}
