package com.rongan.security_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 传感器历史数据实体
 */
@Entity
@Data
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceId;        // 关联的设备 ID
    private Double value;          // 数值
    private String unit;           // 单位 摄氏度 百分比或 ppm
    private LocalDateTime createTime; // 采集时间
}
