package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 短信发送日志
 */
@Entity
@Data
public class SmsLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long alertId;       // 关联的告警 ID
    private String phone;        // 接收手机号
    private String content;      // 短信内容
    private Integer status;      // 发送状态：0 失败，1 成功
    private LocalDateTime createTime; // 发送时间
}
