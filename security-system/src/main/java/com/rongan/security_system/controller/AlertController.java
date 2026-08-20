package com.rongan.security_system.controller;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.entity.Device;
import com.rongan.security_system.repository.AlertRepository;
import com.rongan.security_system.repository.DeviceRepository;
import com.rongan.security_system.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * 告警控制器
 * 处理告警相关的增删改查操作
 * 包括手动触发告警、处理告警、批量处理等功能
 */
@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SmsService smsService;

    private Random random = new Random();

    /**
     * 获取告警列表
     * 按时间倒序排列，默认返回最近500条
     * @param limit 返回条数限制
     * @return 告警列表
     */
    @GetMapping("/list")
    public List<Alert> list(@RequestParam(required = false, defaultValue = "500") int limit) {
        // 创建分页查询条件：第0页，每页limit条，按创建时间倒序
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Alert> page = alertRepository.findAll(pageable);
        return page.getContent();
    }

    /**
     * 手动批量触发告警
     * 仅管理员可操作
     * 随机选择2到3个传感器设备生成模拟告警
     * @param role 用户角色
     * @return 生成的告警列表
     */
    @PostMapping("/manual/batch")
    public ResponseEntity<?> manualAlertBatch(@RequestHeader(value = "X-User-Role", required = false) String role) {
        // 权限校验：仅管理员可操作
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        
        // 随机生成告警数量：2或3个
        int count = random.nextInt(2) + 2;
        List<Alert> alarms = new ArrayList<>();

        // 查询所有传感器设备，排除摄像头
        List<Device> devices = deviceRepository.findAll().stream()
                .filter(d -> !"camera".equals(d.getType()))
                .toList();
        if (devices.isEmpty()) {
            return ResponseEntity.ok(alarms);
        }

        // 随机打乱设备顺序，选取前count个
        List<Device> deviceCopy = new ArrayList<>(devices);
        Collections.shuffle(deviceCopy);
        int selectedCount = Math.min(count, deviceCopy.size());
        
        // 遍历选中设备，生成告警
        for (int i = 0; i < selectedCount; i++) {
            Device device = deviceCopy.get(i);
            Alert alarm = new Alert();
            alarm.setDeviceId(device.getId());
            alarm.setStatus(0); // 状态0表示未处理
            alarm.setCreateTime(LocalDateTime.now());

            // 根据设备类型生成不同告警内容
            String alertType = "";
            String content = "";
            int level = 1; // 默认级别1-一般

            String deviceName = device.getName() != null ? device.getName() : "未知设备";
            String deviceType = device.getType() != null ? device.getType() : "";

            switch (deviceType) {
                case "smoke": // 烟感设备
                    // 生成200到600之间的随机烟雾浓度值
                    double smoke = 200 + random.nextDouble() * 400;
                    alertType = "烟雾超标";
                    content = String.format("%s检测到异常值：%.2f ppm", deviceName, smoke);
                    // 根据浓度值判定告警级别
                    if (smoke > 300) level = Alert.LEVEL_SEVERE;      // 大于300为严重
                    else if (smoke > 200) level = Alert.LEVEL_NORMAL; // 大于200为一般
                    else level = Alert.LEVEL_WARNING;                 // 其他为预警
                    break;
                case "temperature": // 温感设备
                    // 生成60到100之间的随机温度值
                    double temp = 60 + random.nextDouble() * 40;
                    alertType = "高温告警";
                    content = String.format("%s检测到异常值：%.2f C", deviceName, temp);
                    // 根据温度值判定告警级别
                    if (temp > 60) level = Alert.LEVEL_SEVERE;      // 大于60为严重
                    else if (temp > 40) level = Alert.LEVEL_NORMAL; // 大于40为一般
                    else level = Alert.LEVEL_WARNING;               // 其他为预警
                    break;
                default: // 其他设备类型
                    alertType = "设备异常";
                    content = String.format("%s状态异常", deviceName);
                    level = Alert.LEVEL_NORMAL;
                    break;
            }

            alarm.setAlertType(alertType);
            alarm.setContent(content);
            alarm.setLevel(level);
            alarms.add(alarm);
            
            // 保存告警到数据库
            alertRepository.save(alarm);
            // 通过WebSocket推送告警消息到前端
            messagingTemplate.convertAndSend("/topic/alerts", alarm);
            // 发送短信通知
            smsService.sendAlertSms(alarm);
        }
        
        return ResponseEntity.ok(alarms);
    }

    /**
     * 处理单个告警
     * 将告警状态改为已处理
     * @param id 告警ID
     * @param handler 处理人姓名
     * @param role 用户角色
     * @return 处理后的告警
     */
    @PutMapping("/handle/{id}")
    public ResponseEntity<?> handleAlert(@PathVariable Long id, @RequestParam String handler,
                                         @RequestHeader(value = "X-User-Role", required = false) String role) {
        // 权限校验：仅管理员可操作
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        // 查询告警并更新状态
        Alert alert = alertRepository.findById(id).orElseThrow();
        alert.setStatus(1); // 状态1表示已处理
        alert.setHandledTime(LocalDateTime.now());
        alert.setHandler(handler);
        Alert savedAlert = alertRepository.save(alert);
        
        return ResponseEntity.ok(savedAlert);
    }

    /**
     * 一键处理所有未处理告警
     * 批量将所有未处理告警标记为已处理
     * @param handler 处理人姓名
     * @param role 用户角色
     * @return 处理结果，包含处理数量
     */
    @PutMapping("/handleAll")
    @Transactional // 事务注解，保证批量操作原子性
    public ResponseEntity<?> handleAllAlerts(@RequestParam String handler,
                                             @RequestHeader(value = "X-User-Role", required = false) String role) {
        // 权限校验：仅管理员可操作
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        // 查询所有状态为0的未处理告警
        List<Alert> unhandledAlerts = alertRepository.findByStatus(0);
        // 遍历更新每条告警的状态和处理信息
        for (Alert alert : unhandledAlerts) {
            alert.setStatus(1); // 标记为已处理
            alert.setHandledTime(LocalDateTime.now());
            alert.setHandler(handler);
        }
        // 批量保存到数据库
        alertRepository.saveAll(unhandledAlerts);
        
        // 组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", unhandledAlerts.size());
        result.put("message", "已处理 " + unhandledAlerts.size() + " 条告警");
        return ResponseEntity.ok(result);
    }
}
