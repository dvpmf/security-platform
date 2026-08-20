package com.rongan.security_system.controller;

import com.rongan.security_system.entity.Device;
import com.rongan.security_system.entity.SensorData;
import com.rongan.security_system.repository.DeviceRepository;
import com.rongan.security_system.repository.SensorDataRepository;
import com.rongan.security_system.service.SensorIntelligentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传感器数据接口，提供最新数据和历史数据查询
 */
@RestController
@RequestMapping("/api/sensor")
public class SensorDataController {

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SensorIntelligentAnalysisService analysisService;

    /**
     * 获取某个设备的最新一条数据 (最近 7 天内)
     */
    @GetMapping("/latest/{deviceId}")
    public SensorData getLatest(@PathVariable Long deviceId) {
        List<SensorData> list = sensorDataRepository.findByDeviceIdAndCreateTimeAfterOrderByCreateTimeAsc(
                deviceId, LocalDateTime.now().minusDays(7));
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * 获取某个设备最近 7 天的历史数据
     */
    @GetMapping("/history/{deviceId}")
    public List<SensorData> getHistory(@PathVariable Long deviceId) {
        return sensorDataRepository.findByDeviceIdAndCreateTimeAfterOrderByCreateTimeAsc(
                deviceId, LocalDateTime.now().minusDays(7));
    }

    /**
     * 实时 AI 分析指定设备的最新数据
     * @param deviceId 设备ID
     * @return AI 分析结果
     */
    @GetMapping("/ai-analysis/{deviceId}")
    public Map<String, Object> getAIAnalysis(@PathVariable Long deviceId) {
        Map<String, Object> result = new HashMap<>();

        // 获取设备信息
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) {
            result.put("valid", false);
            result.put("message", "设备不存在");
            return result;
        }

        String type = device.getType();
        if (type == null) type = "";
        type = type.trim().toLowerCase();

        // 增强兼容性：支持 "smoke", "烟感", "temperature", "温感"
        boolean isSmoke = type.contains("smoke") || type.contains("烟");
        boolean isTemp = type.contains("temperature") || type.contains("温");

        if (!isSmoke && !isTemp) {
            result.put("valid", false);
            result.put("message", "该设备不支持 AI 分析（仅支持烟感和温感）");
            return result;
        }

        // 获取最近24小时数据
        List<SensorData> history = sensorDataRepository
                .findByDeviceIdAndCreateTimeAfterOrderByCreateTimeAsc(
                        deviceId, LocalDateTime.now().minusHours(24));

        if (history.size() < 5) {
            result.put("valid", false);
            result.put("message", "数据不足，无法进行 AI 分析（至少需要5个数据点）");
            return result;
        }

        // 调用分析服务获取实时分析结果
        Map<String, Object> analysis = analysisService.analyzeDeviceRealTime(device, history);

        result.put("valid", true);
        result.put("level", analysis.get("level"));
        result.put("description", analysis.get("description"));
        result.put("latestValue", history.get(history.size() - 1).getValue());
        result.put("unit", isTemp ? "℃" : "ppm");

        return result;
    }
}
