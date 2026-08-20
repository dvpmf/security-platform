package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 设备实体类
 * 对应数据库表 device
 * 存储安防设备信息，包括摄像头、烟感、温感等
 */
@Entity
@Data
public class Device {

    /**
     * 设备ID
     * 自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 设备名称
     * 如大门摄像头、车间烟感等
     */
    private String name;

    /**
     * 设备类型
     * camera为摄像头
     * smoke为烟感传感器
     * temperature为温感传感器
     */
    private String type;

    /**
     * 安装位置
     * 如大门、车间、仓库等
     */
    private String location;

    /**
     * 设备状态
     * 0为离线，1为在线
     */
    private Integer status;

    /**
     * 最后在线时间
     * 用于判断设备是否超时离线
     */
    private LocalDateTime lastOnlineTime;
}
