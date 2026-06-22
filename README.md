# BJYO

BJYO 是基于 Spring Boot、Apache Shiro、Thymeleaf、MyBatis、Quartz、Druid 的 Java 后台管理脚手架。

## 模块说明

- `bjyo-admin`：Web 启动入口与页面资源
- `bjyo-framework`：核心框架配置、安全、拦截器、异常处理
- `bjyo-system`：系统管理业务模块
- `bjyo-quartz`：定时任务模块
- `bjyo-generator`：代码生成模块
- `bjyo-common`：通用工具、注解、基础实体

## 包名

项目 Java 根包名已调整为：

```text
com.bjyo
```

## 数据库

默认数据库名为 `bjyo`，初始化脚本位于：

```text
sql/bjyo_20260319.sql
```

## 启动

1. 创建数据库 `bjyo` 并导入 `sql/bjyo_20260319.sql`。
2. 修改 `bjyo-admin/src/main/resources/application-druid.yml` 中的数据源账号和密码。
3. 执行构建：

```bash
mvn clean package -DskipTests
```

4. 启动应用：

```bash
java -jar bjyo-admin/target/bjyo-admin.jar
```

默认访问地址由 `bjyo-admin/src/main/resources/application.yml` 中的 `server.port` 和 `server.servlet.context-path` 控制。
