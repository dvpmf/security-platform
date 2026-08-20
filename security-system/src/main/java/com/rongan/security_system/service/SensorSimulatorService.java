package com.rongan.security_system.service;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.entity.Device;
import com.rongan.security_system.entity.SensorData;
import com.rongan.security_system.repository.AlertRepository;
import com.rongan.security_system.repository.DeviceRepository;
import com.rongan.security_system.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class SensorSimulatorService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SmsService smsService;

    private Random random = new Random();

    @Scheduled(fixedDelay = 60000)
    public void generateData() {
        List<Device> devices = deviceRepository.findAll();

        int alertCount = 2 + random.nextInt(2);
        int[] alertGenerated = {0};

        for (Device device : devices) {
            if (device.getStatus() == 0) {
                // 离线设备：记录所有离线告警
                Alert alert = new Alert();
                alert.setDeviceId(device.getId());
                alert.setAlertType("设备异常");
                alert.setContent(String.format("设备 %s 已离线，请管理员对设备异常及时排查。", device.getName()));
                alert.setLevel(Alert.LEVEL_NORMAL);
                alert.setStatus(0);
                alert.setCreateTime(LocalDateTime.now());
                alertRepository.save(alert);
                messagingTemplate.convertAndSend("/topic/alerts", alert);
                smsService.sendAlertSms(alert);
                continue;
            }

            SensorData data = new SensorData();
            data.setDeviceId(device.getId());
            data.setCreateTime(LocalDateTime.now());

            boolean shouldCheckAlert = false;
            double value = 0;
            String unit = "";

            switch (device.getType()) {
                case "temperature":
                    if (alertGenerated[0] < alertCount && alertGenerated[0] < devices.size()) {
                        value = generateTemperatureValue();
                        unit = "℃";
                        shouldCheckAlert = true;
                        alertGenerated[0]++;
                    } else {
                        value = 20 + random.nextDouble() * 20;
                        unit = "℃";
                    }
                    break;
                case "smoke":
                    if (alertGenerated[0] < alertCount && alertGenerated[0] < devices.size()) {
                        value = generateSmokeValue();
                        unit = "ppm";
                        shouldCheckAlert = true;
                        alertGenerated[0]++;
                    } else {
                        value = random.nextDouble() * 150;
                        unit = "ppm";
                    }
                    break;
                default:
                    value = 0;
                    unit = "";
            }

            data.setValue(value);
            data.setUnit(unit);
            sensorDataRepository.save(data);

            if (shouldCheckAlert) {
                checkAndTriggerAlert(device, value, unit);
            }
        }
    }

    private double generateTemperatureValue() {
        double rand = random.nextDouble();
        if (rand < 0.15) {
            return 50 + random.nextDouble() * 5;
        } else if (rand < 0.65) {
            return 40 + random.nextDouble() * 10;
        } else {
            return 25 + random.nextDouble() * 13;
        }
    }

    private double generateSmokeValue() {
        double rand = random.nextDouble();
        if (rand < 0.15) {
            return 300 + random.nextDouble() * 50;
        } else if (rand < 0.65) {
            return 200 + random.nextDouble() * 100;
        } else {
            return 50 + random.nextDouble() * 100;
        }
    }

    private void checkAndTriggerAlert(Device device, double value, String unit) {
        boolean isAlert = false;
        String alertType = null;
        int level = 0;

        if ("temperature".equals(device.getType())) {
            if (value > 60) {
                isAlert = true;
                alertType = "高温告警";
                level = Alert.LEVEL_SEVERE;
            } else if (value > 40) {
                isAlert = true;
                alertType = "温度偏高";
                level = Alert.LEVEL_NORMAL;
            } else if (value > 35) {
                isAlert = true;
                alertType = "温度预警";
                level = Alert.LEVEL_WARNING;
            }
        } else if ("smoke".equals(device.getType())) {
            if (value > 300) {
                isAlert = true;
                alertType = "烟雾超标";
                level = Alert.LEVEL_SEVERE;
            } else if (value > 200) {
                isAlert = true;
                alertType = "烟雾偏高";
                level = Alert.LEVEL_NORMAL;
            } else if (value > 100) {
                isAlert = true;
                alertType = "烟雾预警";
                level = Alert.LEVEL_WARNING;
            }
        }

        if (isAlert) {
            // 记录所有触发的告警 不再检查是否有未处理的相同类型告警
            Alert alert = new Alert();
            alert.setDeviceId(device.getId());
            alert.setAlertType(alertType);
            alert.setContent(String.format("%s 检测到异常值：%.2f %s", device.getName(), value, unit));
            alert.setLevel(level);
            alert.setStatus(0);
            alert.setCreateTime(LocalDateTime.now());
            alertRepository.save(alert);
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            smsService.sendAlertSms(alert);
        }
    }
}
