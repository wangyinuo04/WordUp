\# WordUp 项目结构与开发协作规范



\## 一、 项目概述与架构说明



本项目（WordUp）采用单仓库（Monorepo）模式管理，将 Android 移动端（前端）与 Java 服务端（后端）代码统一托管于同一个 Git 仓库中。此架构旨在方便全栈查阅与统一版本追踪。



为确保团队协作时不同开发环境的配置不发生冲突，仓库已配置全局 `.gitignore`，严格过滤了本地 IDE 缓存、编译产物及临时测试数据。



\### 目录结构图示



克隆仓库后，您的本地文件系统应呈现如下标准结构：



```text

WordUp/ (Git 仓库根目录)

├── .gitignore              # 全局忽略规则配置文件（严禁随意修改）

├── README.md               # 项目开发与协作说明文档

├── app-frontend/           # Android 前端工程目录（基于 Gradle 构建）

│   ├── app/                # 具体的 Android 业务代码与资源模块

│   ├── gradle/             # 包含 libs.versions.toml 依赖版本管理

│   ├── build.gradle.kts    # 根项目构建脚本

│   └── settings.gradle.kts # 模块化配置声明

└── app-backend/            # Java 后端工程目录（基于 Maven 构建）

&#x20;   ├── src/                # 后端业务源码与配置文件

&#x20;   ├── pom.xml             # Maven 依赖配置文件

&#x20;   └── uploads/            # 业务数据挂载目录（仅保留 .gitkeep，忽略本地测试图片）

