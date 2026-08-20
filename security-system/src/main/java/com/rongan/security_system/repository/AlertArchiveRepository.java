package com.rongan.security_system.repository;

import com.rongan.security_system.entity.AlertArchive;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 告警归档数据访问接口
 * 继承 JpaRepository 提供基础 CRUD 操作
 * 用于访问 alert_archive 表的历史告警数据
 */
public interface AlertArchiveRepository extends JpaRepository<AlertArchive, Long> {
}
