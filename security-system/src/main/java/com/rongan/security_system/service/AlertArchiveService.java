package com.rongan.security_system.service;

import com.rongan.security_system.entity.Alert;
import com.rongan.security_system.entity.AlertArchive;
import com.rongan.security_system.repository.AlertRepository;
import com.rongan.security_system.repository.AlertArchiveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 告警滚动归档服务
 * 定时将旧告警从主表迁移到归档表
 * 减轻主表查询压力，保留历史数据
 */
@Service
public class AlertArchiveService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertArchiveRepository archiveRepository;

    /**
     * 定时归档旧告警
     * 每5分钟执行一次检查
     * 当主表超过500条时，保留最新100条，其余归档
     */
    @Scheduled(fixedDelay = 300000)   // 每5分钟执行一次
    @Transactional
    public void archiveOldAlerts() {
        long total = alertRepository.count();
        final long MAX_MAIN_TABLE = 500;
        final long KEEP_LATEST = 100;

        if (total > MAX_MAIN_TABLE) {
            long toArchive = total - KEEP_LATEST;

            // 查询最旧的待归档告警，按创建时间升序取前 toArchive 条
            List<Alert> oldAlerts = alertRepository.findAll(
                    PageRequest.of(0, (int) toArchive, Sort.by(Sort.Direction.ASC, "createTime"))
            ).getContent();

            // 批量转换为归档对象并保存
            List<AlertArchive> archives = oldAlerts.stream()
                    .map(AlertArchive::from)
                    .toList();
            archiveRepository.saveAll(archives);

            // 从主表删除已归档记录
            alertRepository.deleteAll(oldAlerts);

            System.out.println("归档完成：迁移 " + archives.size() + " 条旧告警，主表保留 " + (total - toArchive) + " 条");
        }
    }
}
