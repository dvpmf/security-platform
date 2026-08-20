package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警记录实体类
 * 对应数据库表 alert
 * 存储系统产生的各类告警信息
 */
@Entity
@Data
public class Alert {

    /**
     * 告警ID
     * 自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联设备ID
     * 手动告警时可为空
     */
    private Long deviceId;

    /**
     * 告警类型
     * 如高温告警、烟雾超标、设备离线等
     */
    private String alertType;

    /**
     * 告警内容
     * 详细描述告警信息
     */
    private String content;

    /**
     * 告警级别
     * 1为预警，2为一般，3为严重
     */
    private Integer level;

    /**
     * 处理状态
     * 0为未处理，1为已处理
     */
    private Integer status;

    /**
     * 告警发生时间
     */
    private LocalDateTime createTime;

    /**
     * 告警处理时间
     */
    private LocalDateTime handledTime;

    /**
     * 处理人姓名
     */
    private String handler;

    // ========== 告警级别常量 ==========

    /**
     * 预警级别
     * 轻微异常，需要关注
     */
    public static final int LEVEL_WARNING = 1;

    /**
     * 一般级别
     * 需要处理的异常
     */
    public static final int LEVEL_NORMAL = 2;

    /**
     * 严重级别
     * 紧急异常，需立即处理
     */
    public static final int LEVEL_SEVERE = 3;
}
