package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警归档实体类
 * 对应数据库表 alert_archive
 * 存储从 alert 表迁移的历史告警记录
 * 用于减轻主表查询压力，保留历史数据
 */
@Entity
@Data
@Table(name = "alert_archive")
public class AlertArchive {

    /**
     * 归档记录ID
     * 自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 原告警ID
     * 对应 alert 表的原记录ID，便于追溯
     */
    private Long originalAlertId;

    /**
     * 关联设备ID
     * 产生告警的设备标识
     */
    private Long deviceId;

    /**
     * 告警类型
     * 如温度AI预警、烟雾AI预警、设备异常等
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
     * 归档时通常为已处理状态
     * 0为未处理，1为已处理
     */
    private Integer status;

    /**
     * 处理人姓名
     */
    private String handler;

    /**
     * 告警发生时间
     */
    private LocalDateTime createTime;

    /**
     * 告警处理时间
     */
    private LocalDateTime handledTime;

    /**
     * 归档时间
     * 记录该条告警被归档的时间
     */
    private LocalDateTime archivedTime;

    /**
     * 从 Alert 对象创建归档对象
     * 复制所有字段并设置归档时间
     * @param alert 待归档的告警对象
     * @return 归档对象
     */
    public static AlertArchive from(Alert alert) {
        AlertArchive archive = new AlertArchive();
        archive.setOriginalAlertId(alert.getId());
        archive.setDeviceId(alert.getDeviceId());
        archive.setAlertType(alert.getAlertType());
        archive.setContent(alert.getContent());
        archive.setLevel(alert.getLevel());
        archive.setStatus(alert.getStatus());
        archive.setHandler(alert.getHandler());
        archive.setCreateTime(alert.getCreateTime());
        archive.setHandledTime(alert.getHandledTime());
        archive.setArchivedTime(LocalDateTime.now());
        return archive;
    }
}
