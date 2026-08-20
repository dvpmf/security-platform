# 工业园区智慧安防与应急联动系统

毕业设计项目。基于 B/S 架构的智慧安防 Web 平台，实现园区设备的统一监控、
异常告警的实时推送，以及告警发生后的应急预案联动处置。

## 技术栈

**后端**
- Java 17
- Spring Boot 3.5.11
- Spring Data JPA
- Spring Security
- Spring WebSocket
- MySQL
- Lombok

**前端**
- Vue 3.2
- Vue CLI 5
- Element Plus 2.13
- ECharts 6
- Axios
- StompJS + SockJS（WebSocket）

## 功能模块

- 用户登录认证（Spring Security + BCrypt 加密）
- 设备台账管理
- 传感器实时数据监控
- 异常告警实时推送（WebSocket）
- 告警统计与数据可视化
- 应急预案管理
- 应急演练联动
- 摄像头监控
- 短信通知（模拟模式）

## 项目结构

├── security-system/    # Spring Boot 后端
│   └── src/main/java/com/rongan/security_system/
│       ├── config/       # 安全、跨域、WebSocket 配置
│       ├── controller/   # 接口层
│       ├── entity/       # 实体类
│       ├── repository/   # 数据访问层（JPA）
│       └── service/      # 业务逻辑层
└── security-frontend/   # Vue3 前端
    └── src/
        ├── components/   # 页面组件
        ├── router/       # 路由
        ├── App.vue
        └── main.js

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 18+
- MySQL 8.0

## 快速开始

### 1. 初始化数据库

```sql
CREATE DATABASE security_db DEFAULT CHARACTER SET utf8mb4;
```

### 2. 启动后端

修改 `security-system/src/main/resources/application.properties` 中的数据库账号密码，
默认读取环境变量 `DB_PASSWORD`（默认 `root123`）。

```bash
cd security-system
mvnw spring-boot:run
```

后端默认端口：`8081`

### 3. 启动前端

```bash
cd security-frontend
npm install
npm run serve
```

前端开发服务器默认端口：`8082`，已配置代理转发 `/api` 和 `/ws` 到后端 `8081`。

## 作者 马福

2026 届 计算机科学与技术 长春电子科技学院
