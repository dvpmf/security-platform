package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 应急日志实体，记录每次应急响应的详细信息
 */
@Entity
@Data
public class EmergencyLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long alertId;          // 关联的告警ID
    private String alertType;      // 告警类型 例如温度AI预警
    private String planTitle;      // 匹配的预案标题 可能为空
    private String operator;       // 操作人 系统自动或管理员
    private LocalDateTime executeTime;  // 执行时间
}
