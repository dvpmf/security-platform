package com.rongan.security_system.repository;

import com.rongan.security_system.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    // 查询某个设备在指定时间之后的数据，按时间升序排列
    List<SensorData> findByDeviceIdAndCreateTimeAfterOrderByCreateTimeAsc(Long deviceId, LocalDateTime after);
}
