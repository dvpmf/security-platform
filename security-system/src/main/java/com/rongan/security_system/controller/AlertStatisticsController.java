package com.rongan.security_system.controller;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警统计控制器 - 提供告警数据统计接口
 */
@RestController
@RequestMapping("/api/alert")
public class AlertStatisticsController {

    @Autowired
    private AlertRepository alertRepository;

    /**
     * 获取告警统计数据
     * @return 统计数据
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 1. 基本统计
        long total = alertRepository.count();
        long unhandled = alertRepository.countByStatus(0);
        long handled = alertRepository.countByStatus(1);
        stats.put("total", total);
        stats.put("unhandled", unhandled);
        stats.put("handled", handled);
        
        // 2. 按等级统计
        stats.put("warningCount", alertRepository.countByLevel(Alert.LEVEL_WARNING));
        stats.put("normalCount", alertRepository.countByLevel(Alert.LEVEL_NORMAL));
        stats.put("severeCount", alertRepository.countByLevel(Alert.LEVEL_SEVERE));
        
        // 3. 近 7 天每日告警趋势
        LocalDateTime start = LocalDate.now().minusDays(6).atStartOfDay();
        List<Object[]> dayCounts = alertRepository.countByDay(start);
        
        // 转换为 Map<日期字符串，数量>
        Map<String, Long> countMap = dayCounts.stream()
            .collect(Collectors.toMap(
                arr -> ((java.sql.Date) arr[0]).toLocalDate().toString(),
                arr -> (Long) arr[1]
            ));
        
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(6 - i);
            String dateStr = date.toString();
            dates.add(dateStr);
            counts.add(countMap.getOrDefault(dateStr, 0L));
        }
        stats.put("trendDates", dates);
        stats.put("trendCounts", counts);
        
        // 3. 四个设备的告警数量 + 设备异常
        List<String> deviceNames = Arrays.asList("仓库温感", "仓库烟感", "车间温感", "车间烟感");
        List<Object[]> deviceCounts = alertRepository.countByDeviceNames(deviceNames);
        Map<String, Long> deviceMap = deviceCounts.stream()
            .collect(Collectors.toMap(
                arr -> (String) arr[0],
                arr -> (Long) arr[1]
            ));
        
        // 新增：统计设备异常告警数量
        long deviceExceptionCount = alertRepository.countByAlertType("设备异常");
        
        List<Map<String, Object>> typeDistribution = new ArrayList<>();
        for (String name : deviceNames) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", deviceMap.getOrDefault(name, 0L));
            typeDistribution.add(item);
        }
        // 新增：如果设备异常数量大于0，则加入饼图
        if (deviceExceptionCount > 0) {
            Map<String, Object> exceptionItem = new HashMap<>();
            exceptionItem.put("name", "设备异常");
            exceptionItem.put("value", deviceExceptionCount);
            typeDistribution.add(exceptionItem);
        }
        stats.put("typeDistribution", typeDistribution);
        
        return stats;
    }
}
