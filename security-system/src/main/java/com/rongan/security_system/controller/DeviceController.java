package com.rongan.security_system.controller;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.entity.Device;
import com.rongan.security_system.repository.AlertRepository;
import com.rongan.security_system.repository.DeviceRepository;
import com.rongan.security_system.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备控制器
 * 处理设备的增删改查操作
 * 包含设备状态监控和离线告警功能
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SmsService smsService;

    /**
     * 获取所有设备列表
     * @return 设备列表
     */
    @GetMapping("/list")
    public List<Device> list() {
        return deviceRepository.findAll();
    }

    /**
     * 添加新设备
     * 仅管理员可操作
     * @param device 设备信息
     * @param role 用户角色
     * @return 添加后的设备
     */
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Device device,
                                 @RequestHeader(value = "X-User-Role", required = false) String role) {
        // 权限校验
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        return ResponseEntity.ok(deviceRepository.save(device));
    }

    /**
     * 更新设备信息
     * 仅管理员可操作
     * 设备从在线变为离线时自动生成告警
     * @param device 设备信息
     * @param role 用户角色
     * @return 更新后的设备
     */
    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody Device device,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        // 权限校验
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        
        // 查询设备原状态
        Device oldDevice = deviceRepository.findById(device.getId()).orElse(null);
        boolean wasOnline = oldDevice != null && oldDevice.getStatus() == 1;
        boolean willBeOffline = device.getStatus() == 0;

        // 保存更新
        Device saved = deviceRepository.save(device);

        // 设备从在线变为离线，生成离线告警
        if (wasOnline && willBeOffline) {
            Alert alert = new Alert();
            alert.setDeviceId(device.getId());
            alert.setAlertType("设备异常");
            alert.setContent(String.format("设备 %s 已离线，请管理员对设备异常及时排查", device.getName()));
            alert.setLevel(Alert.LEVEL_NORMAL);
            alert.setStatus(0); // 未处理
            alert.setCreateTime(LocalDateTime.now());
            alertRepository.save(alert);
        
            // WebSocket实时推送告警
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            // 发送短信通知
            smsService.sendAlertSms(alert);
        }

        return ResponseEntity.ok(saved);
    }

    /**
     * 删除设备
     * 仅管理员可操作
     * @param id 设备ID
     * @param role 用户角色
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        // 权限校验
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        deviceRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
