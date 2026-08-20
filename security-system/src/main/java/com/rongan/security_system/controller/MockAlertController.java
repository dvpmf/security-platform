package com.rongan.security_system.controller;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.entity.AlertResponse;
import com.rongan.security_system.entity.EmergencyPlan;
import com.rongan.security_system.repository.AlertRepository;
import com.rongan.security_system.repository.EmergencyPlanRepository;
import com.rongan.security_system.service.EmergencyPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 模拟告警控制器 - 用于应急演练
 */
@RestController
@RequestMapping("/api/alert")
public class MockAlertController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmergencyPlanRepository planRepository;

    @Autowired
    private EmergencyPlanService emergencyPlanService;

    @Autowired
    private AlertRepository alertRepository;

    private Random random = new Random();

    /**
     * 发送模拟告警（用于应急演练）
     * 保存演练告警到数据库，并推送WebSocket消息
     * @param role 用户角色
     * @return 包含预案信息的告警响应
     */
    @PostMapping("/mock")
    public ResponseEntity<?> mockAlert(@RequestHeader(value = "X-User-Role", required = false) String role) {
        // 权限校验：仅管理员可操作
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }

        // 查询所有应急预案
        List<EmergencyPlan> plans = planRepository.findAll();
        String planAlertType = "高温告警";
        String content = "这是一条模拟告警，用于应急演练";

        // 随机选择一个预案
        if (!plans.isEmpty()) {
            EmergencyPlan randomPlan = plans.get(random.nextInt(plans.size()));
            planAlertType = randomPlan.getAlertType();
            content = String.format("【模拟演练】触发 %s 告警，请按应急预案处理", planAlertType);
        }

        // 保存演练告警到数据库，告警记录里就会出现这条数据
        Alert mockAlert = new Alert();
        mockAlert.setDeviceId(null);
        mockAlert.setAlertType("演练");
        mockAlert.setContent(content);
        mockAlert.setLevel(1);
        mockAlert.setStatus(0);
        mockAlert.setCreateTime(LocalDateTime.now());
        alertRepository.save(mockAlert);

        // 根据预案类型查找应急预案
        EmergencyPlan matchedPlan = emergencyPlanService.findByAlertType(planAlertType);

        // 构建推送消息，id=-1表示演练告警，前端收到会弹应急面板
        AlertResponse response = new AlertResponse();
        response.setId(-1L);
        response.setAlertType("演练");
        response.setContent(content);
        response.setCreateTime(mockAlert.getCreateTime());
        response.setLevel(1);
        response.setStatus(0);
        if (matchedPlan != null) {
            response.setPlanTitle(matchedPlan.getTitle());
            response.setPlanSteps(matchedPlan.getSteps());
        }

        // WebSocket 推送
        messagingTemplate.convertAndSend("/topic/alerts", response);

        return ResponseEntity.ok(response);
    }
}
