package com.rongan.security_system.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警响应对象 - 用于 WebSocket 推送和 API 返回
 * 包含告警信息及关联的应急预案信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long id;                    // 告警ID 演练告警为-1
    private String alertType;           // 告警类型
    private String content;             // 告警内容
    private LocalDateTime createTime;   // 创建时间
    private Integer level;              // 告警级别
    private Integer status;             // 处理状态
    
    // 关联的应急预案信息
    private String planTitle;           // 预案标题
    private String planSteps;           // 预案步骤
}
