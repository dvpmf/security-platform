package com.rongan.security_system.service;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.entity.EmergencyLog;
import com.rongan.security_system.entity.SmsLog;
import com.rongan.security_system.entity.User;
import com.rongan.security_system.repository.SmsLogRepository;
import com.rongan.security_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * 短信服务
 * 模拟发送告警短信和应急通知短信
 * 实际项目中可替换为真实短信网关
 */
@Service
public class SmsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SmsLogRepository smsLogRepository;

    // 是否启用模拟模式，true只打印不真正发送
    @Value("${sms.mock.enabled}")
    private boolean mockEnabled;

    /**
     * 生成6位随机验证码
     * @return 6位数字字符串
     */
    private String generateRandomCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    /**
     * 发送告警短信
     * 向所有管理员发送告警通知
     * @param alert 告警信息
     */
    public void sendAlertSms(Alert alert) {
        // 查询所有管理员
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> "admin".equals(u.getRole()))
                .toList();

        for (User admin : admins) {
            String phone = admin.getPhone();
            // 手机号为空则跳过
            if (phone == null || phone.trim().isEmpty()) {
                System.out.println("管理员 " + admin.getUsername() + " 手机号为空，跳过");
                continue;
            }
            
            // 生成验证码并组装短信内容
            String code = generateRandomCode();
            String content = String.format("【告警】%s，验证码：%s，请立即处理", alert.getContent(), code);

            // 模拟模式：打印到控制台
            if (mockEnabled) {
                System.out.println("[模拟短信] 发送到 " + phone + " 内容：" + content);
            }

            // 记录短信日志
            SmsLog log = new SmsLog();
            log.setAlertId(alert.getId());
            log.setPhone(phone);
            log.setContent(content);
            log.setStatus(1); // 发送成功
            log.setCreateTime(LocalDateTime.now());
            smsLogRepository.save(log);
        }
    }

    /**
     * 发送应急短信
     * 向所有管理员发送应急通知
     * @param emergencyLog 应急日志
     */
    public void sendEmergencySms(EmergencyLog emergencyLog) {
        // 查询所有管理员
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> "admin".equals(u.getRole()))
                .toList();

        for (User admin : admins) {
            String phone = admin.getPhone();
            // 手机号为空则跳过
            if (phone == null || phone.trim().isEmpty()) {
                continue;
            }
            
            // 生成验证码并组装短信内容
            String code = generateRandomCode();
            String content = String.format("【应急】%s已触发，验证码：%s，请立即处理", 
                    emergencyLog.getPlanTitle(), code);

            // 模拟模式：打印到控制台
            if (mockEnabled) {
                System.out.println("[模拟应急短信] 发送到 " + phone + " 内容：" + content);
            }

            // 记录短信日志
            SmsLog log = new SmsLog();
            log.setAlertId(emergencyLog.getAlertId());
            log.setPhone(phone);
            log.setContent(content);
            log.setStatus(1);
            log.setCreateTime(LocalDateTime.now());
            smsLogRepository.save(log);
        }
    }

    /**
     * 发送全员通知短信
     * 向所有用户发送通知
     * @param message 通知内容
     */
    public void sendNoticeToAllUsers(String message) {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            String phone = user.getPhone();
            // 手机号有效才发送
            if (phone != null && !phone.trim().isEmpty()) {
                String code = generateRandomCode();
                String content = String.format("【应急通知】%s 验证码：%s", message, code);
                
                // 模拟模式：打印到控制台
                if (mockEnabled) {
                    System.out.println("[模拟短信] 发送给 " + phone + "：" + content);
                }
                
                // 记录短信日志
                SmsLog log = new SmsLog();
                log.setAlertId(null);
                log.setPhone(phone);
                log.setContent(content);
                log.setStatus(1);
                log.setCreateTime(LocalDateTime.now());
                smsLogRepository.save(log);
            }
        }
    }
}
