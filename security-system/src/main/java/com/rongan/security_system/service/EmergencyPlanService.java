package com.rongan.security_system.service;

import com.rongan.security_system.entity.EmergencyPlan;
import com.rongan.security_system.repository.EmergencyPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应急预案服务 - 管理不同告警类型的应急预案
 */
@Service
public class EmergencyPlanService {

    @Autowired
    private EmergencyPlanRepository planRepository;

    /**
     * 查询所有应急预案
     * @return 预案列表
     */
    public List<EmergencyPlan> getAllPlans() {
        return planRepository.findAll();
    }

    /**
     * 保存应急预案
     * @param plan 预案对象
     * @return 保存后的预案
     */
    public EmergencyPlan savePlan(EmergencyPlan plan) {
        return planRepository.save(plan);
    }

    /**
     * 删除应急预案
     * @param id 预案 ID
     */
    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    /**
     * 根据告警类型精确匹配查询应急预案
     * @param alertType 告警类型
     * @return 匹配的应急预案，未找到返回 null
     */
    public EmergencyPlan findByAlertType(String alertType) {
        if (alertType == null || alertType.trim().isEmpty()) {
            return null;
        }
        // 从数据库查询 alertType 字段完全匹配的预案
        return planRepository.findByAlertType(alertType.trim());
    }
}
