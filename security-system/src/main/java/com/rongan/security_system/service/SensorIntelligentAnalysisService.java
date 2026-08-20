package com.rongan.security_system.service;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.entity.Device;
import com.rongan.security_system.entity.EmergencyLog;
import com.rongan.security_system.entity.EmergencyPlan;
import com.rongan.security_system.entity.SensorData;
import com.rongan.security_system.repository.AlertRepository;
import com.rongan.security_system.repository.DeviceRepository;
import com.rongan.security_system.repository.EmergencyLogRepository;
import com.rongan.security_system.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器智能分析服务
 * 基于统计学和阈值算法实现传感器数据异常检测
 * 包含固定阈值检测和动态基线检测两种模式
 */
@Service
public class SensorIntelligentAnalysisService {

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SmsService smsService;

    @Autowired
    private EmergencyLogRepository emergencyLogRepository;

    @Autowired
    private EmergencyPlanService emergencyPlanService;

    // 标准差倍数，用于动态异常判定，默认2.5倍
    @Value("${ai.sensor.stddev.multiplier:2.5}")
    private double stddevMultiplier;

    // 最少数据点数，低于此值不进行动态分析，默认5条
    @Value("${ai.sensor.min-data-points:5}")
    private int minDataPoints;

    /**
     * 服务初始化方法
     * 打印配置参数到控制台
     */
    @PostConstruct
    public void init() {
        System.out.println("========================================");
        System.out.println("AI传感器智能分析配置参数加载完成");
        System.out.println("标准差倍数: " + stddevMultiplier);
        System.out.println("最少数据点数: " + minDataPoints);
        System.out.println("========================================");
    }

    /**
     * 定时分析任务
     * 每60秒执行一次，对所有传感器进行异常检测
     */
    @Scheduled(fixedDelay = 60000)
    public void analyzeSensorAnomalies() {
        // 查询所有温度传感器和烟雾传感器
        List<Device> sensors = deviceRepository.findAll().stream()
                .filter(d -> "temperature".equals(d.getType()) || "smoke".equals(d.getType()))
                .toList();
        // 逐个分析每个传感器
        for (Device device : sensors) {
            analyzeDevice(device);
        }
    }

    /**
     * 分析单个传感器
     * 查询历史数据，进行三级阈值检测和动态异常检测
     * @param device 传感器设备
     */
    private void analyzeDevice(Device device) {
        // 查询最近24小时的历史数据
        List<SensorData> history = sensorDataRepository
                .findByDeviceIdAndCreateTimeAfterOrderByCreateTimeAsc(
                        device.getId(),
                        LocalDateTime.now().minusHours(24));
        // 数据点不足，跳过分析
        if (history.size() < minDataPoints) return;

        // 执行三级阈值检测，返回告警级别 0正常 1预警 2一般 3严重
        int thresholdLevel = checkThreeLevelThreshold(history, device.getType());
        // 执行动态异常检测
        boolean dynamicAlert = detectDynamicAnomaly(history, device.getType());

        // 阈值检测或动态检测触发则生成告警
        if (thresholdLevel > 0 || dynamicAlert) {
            // 取更严重的级别
            int finalLevel = Math.max(thresholdLevel, dynamicAlert ? 1 : 0);

            // 检查是否已有同级别的未处理AI预警，避免同一级别重复告警
            // 但允许级别升级（如从预警升级到严重）
            boolean hasSameLevelUnresolved = alertRepository.findByStatus(0).stream()
                    .filter(a -> a.getDeviceId().equals(device.getId()))
                    .filter(a -> a.getAlertType().contains("AI预警"))
                    .anyMatch(a -> a.getLevel() == finalLevel);

            if (hasSameLevelUnresolved) {
                // 同一级别已有未处理告警，跳过
                return;
            }

            String reason = generateReason(device, thresholdLevel, dynamicAlert, history);
            createAIPredictionAlert(device, reason, finalLevel);
        }
    }

    /**
     * 三级阈值检测
     * 根据设备类型使用三级阈值进行判定
     * @param data 传感器历史数据
     * @param type 设备类型
     * @return 告警级别 0正常 1预警 2一般 3严重
     */
    private int checkThreeLevelThreshold(List<SensorData> data, String type) {
        if (data.isEmpty()) return 0;
        // 获取最新数据点
        double latest = data.get(data.size() - 1).getValue();

        if ("temperature".equals(type)) {
            // 温度三级阈值
            if (latest > 70) return 3;      // 严重 >70℃
            if (latest > 50) return 2;      // 一般 50-70℃
            if (latest > 35) return 1;      // 预警 35-50℃
            return 0;                       // 正常 ≤35℃
        }

        if ("smoke".equals(type)) {
            // 烟雾三级阈值
            if (latest > 300) return 3;     // 严重 >300ppm
            if (latest > 200) return 2;     // 一般 200-300ppm
            if (latest > 100) return 1;     // 预警 100-200ppm
            return 0;                       // 正常 ≤100ppm
        }

        return 0;
    }

    /**
     * 动态异常检测
     * 基于统计学标准差算法，检测偏离历史基线的异常值
     * @param data 传感器历史数据
     * @param type 设备类型
     * @return true表示触发告警
     */
    private boolean detectDynamicAnomaly(List<SensorData> data, String type) {
        // 排除最新点，用于计算历史基线
        int excludeLast = 1;
        // 数据量不足，无法计算标准差
        if (data.size() <= excludeLast + minDataPoints) return false;

        // 计算历史数据均值，排除最后一个点
        double sum = 0;
        for (int i = 0; i < data.size() - excludeLast; i++) {
            sum += data.get(i).getValue();
        }
        double mean = sum / (data.size() - excludeLast);

        // 计算历史数据标准差
        double variance = 0;
        for (int i = 0; i < data.size() - excludeLast; i++) {
            variance += Math.pow(data.get(i).getValue() - mean, 2);
        }
        double stddev = Math.sqrt(variance / (data.size() - excludeLast));

        // 标准差过小说明数据稳定，不触发预警
        if (stddev < 0.01) return false;

        // 获取最新数据点
        double latest = data.get(data.size() - 1).getValue();
        
        // 设置数值下限，避免正常环境微小波动触发误报
        // 温度下限35度，烟雾下限100ppm
        double minValue = "temperature".equals(type) ? 35.0 : 100.0;
        if (latest < minValue) return false;
        
        // 判定最新值是否偏离均值超过stddevMultiplier个标准差
        return Math.abs(latest - mean) > stddevMultiplier * stddev;
    }

    /**
     * 生成告警原因描述
     * @param device 传感器设备
     * @param thresholdLevel 阈值检测级别 0正常 1预警 2一般 3严重
     * @param dynamic 是否触发动态异常
     * @param history 历史数据
     * @return 告警原因文本
     */
    private String generateReason(Device device, int thresholdLevel, boolean dynamic, List<SensorData> history) {
        double latest = history.get(history.size() - 1).getValue();
        String unit = device.getType().equals("temperature") ? "℃" : "ppm";

        // 根据阈值级别生成描述
        switch (thresholdLevel) {
            case 3:
                return String.format("【AI严重预警】%s数值超过安全阈值，当前%.1f%s，请管理员立即处理", device.getName(), latest, unit);
            case 2:
                return String.format("【AI一般预警】%s数值异常，当前%.1f%s，请管理员及时排查", device.getName(), latest, unit);
            case 1:
                return String.format("【AI预警】%s数值偏高，当前%.1f%s，建议管理员关注", device.getName(), latest, unit);
            default:
                // 只有动态异常触发
                if (dynamic) {
                    return String.format("【AI趋势预警】%s数值偏离历史基线，当前%.1f%s，建议管理员排查", device.getName(), latest, unit);
                }
                return String.format("【AI预警】%s检测到异常，当前%.1f%s", device.getName(), latest, unit);
        }
    }

    /**
     * 创建AI预警告警
     * 保存告警到数据库，推送WebSocket消息，发送短信通知
     * @param device 传感器设备
     * @param reason 告警原因
     * @param level 告警级别
     */
    private void createAIPredictionAlert(Device device, String reason, int level) {
        Alert alert = new Alert();
        alert.setDeviceId(device.getId());
        alert.setAlertType(device.getType().equals("temperature") ? "温度AI预警" : "烟雾AI预警");
        alert.setContent(reason);
        alert.setLevel(level);
        alert.setStatus(0);
        alert.setCreateTime(LocalDateTime.now());
        
        // 保存告警
        alertRepository.save(alert);
        
        // 查询关联的应急预案
        String planAlertType = alert.getAlertType();
        if ("温度AI预警".equals(planAlertType)) {
            planAlertType = "高温告警";
        } else if ("烟雾AI预警".equals(planAlertType)) {
            planAlertType = "烟雾超标";
        }
        EmergencyPlan plan = emergencyPlanService.findByAlertType(planAlertType);
        
        // 构建包含预案信息的告警数据
        java.util.Map<String, Object> alertData = new java.util.HashMap<>();
        alertData.put("id", alert.getId());
        alertData.put("deviceId", alert.getDeviceId());
        alertData.put("alertType", alert.getAlertType());
        alertData.put("content", alert.getContent());
        alertData.put("level", alert.getLevel());
        alertData.put("status", alert.getStatus());
        alertData.put("createTime", alert.getCreateTime().toString());
        if (plan != null) {
            alertData.put("planTitle", plan.getTitle());
            alertData.put("planSteps", plan.getSteps());
        }
        
        // WebSocket实时推送 包含预案信息
        messagingTemplate.convertAndSend("/topic/alerts", alertData);
        
        // 发送短信通知
        smsService.sendAlertSms(alert);
        
        // 自动触发应急预案
        autoTriggerEmergencyPlan(device, alert);
    }

    /**
     * 自动触发应急预案
     * 根据告警类型映射查找对应预案并执行应急流程
     * @param device 传感器设备
     * @param alert 告警信息
     */
    private void autoTriggerEmergencyPlan(Device device, Alert alert) {
        // 步骤1 将AI预警类型映射为预案表中的告警类型
        String planAlertType = alert.getAlertType();
        if ("温度AI预警".equals(planAlertType)) {
            planAlertType = "高温告警";
        } else if ("烟雾AI预警".equals(planAlertType)) {
            planAlertType = "烟雾超标";
        }

        // 步骤2 用已有的 findByAlertType 查找预案
        EmergencyPlan plan = emergencyPlanService.findByAlertType(planAlertType);
        if (plan == null) {
            // 没找到预案就不处理
            return;
        }

        // 步骤3 记录应急日志 只用 EmergencyLog 里已有的字段
        EmergencyLog log = new EmergencyLog();
        log.setAlertId(alert.getId());
        log.setAlertType(alert.getAlertType());
        log.setPlanTitle(plan.getTitle());
        log.setOperator("系统自动");
        log.setExecuteTime(LocalDateTime.now());
        emergencyLogRepository.save(log);

        // 步骤4 发短信通知 复用已有的发告警短信方法
        smsService.sendAlertSms(alert);

        // 步骤5 通知前端 使用已有的告警 Topic 不需要新建
        messagingTemplate.convertAndSend("/topic/alerts", "应急预案已触发：" + plan.getTitle());
    }

    /**
     * 实时分析设备数据
     * 供前端实时查询设备AI分析结果使用
     * @param device 传感器设备
     * @param history 历史数据列表
     * @return 分析结果Map 包含level和description
     */
    public java.util.Map<String, Object> analyzeDeviceRealTime(Device device, List<SensorData> history) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (history.size() < minDataPoints) {
            result.put("level", "normal");
            result.put("description", "数据不足 无法进行 AI 分析");
            return result;
        }
        int thresholdLevel = checkThreeLevelThreshold(history, device.getType());
        boolean dynamic = detectDynamicAnomaly(history, device.getType());
        int level = 0;
        String description = "设备运行正常";
        if (thresholdLevel > 0) {
            level = thresholdLevel;
            description = generateReason(device, thresholdLevel, dynamic, history);
        } else if (dynamic) {
            level = 1;
            description = generateReason(device, 0, true, history);
        }
        // 将数字级别映射为字符串 供前端使用
        String levelStr;
        switch (level) {
            case 3:
                levelStr = "danger";
                break;
            case 2:
                levelStr = "warning";
                break;
            case 1:
                levelStr = "normal";
                break;
            default:
                levelStr = "normal";
        }
        result.put("level", levelStr);
        result.put("description", description);
        return result;
    }
}
