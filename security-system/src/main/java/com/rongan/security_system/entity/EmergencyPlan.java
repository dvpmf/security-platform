package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 应急预案实体 - 存储不同告警类型的应急预案
 */
@Entity
@Data
public class EmergencyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String alertType;      // 告警类型 例如烟雾超标 高温告警
    private String title;          // 预案标题
    private String steps;          // 处理步骤
    private String notifyRoles;    // 通知角色 可能为空
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
