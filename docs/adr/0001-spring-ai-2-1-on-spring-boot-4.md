# 采用 Spring AI 2.1.0 GA 与 Spring Boot 4.2.x 技术基线

本项目是面向未来的全新学习型仓库，我们选择直接采用 **Spring AI 2.1.0 GA + Spring Boot 4.2.x + JDK 21**，而非仍广泛存在于旧教程与存量系统中的 Spring AI 1.1.x + Spring Boot 3.5.x。Spring AI 2.x 无法在 Spring Boot 3.x 中运行，但将工具调用循环迁移至客户端 `ToolCallingAdvisor`、以追加式事件日志 `Session API` 取代已弃用的 `ChatMemory`，并原生提供人工确认（Tool Call Confirmation）与无状态 MCP 支持；跳过 1.x 可避免初级开发者先学习已弃用 API 再返工，代价是学习期间需以官方一手文档和源码而非早期中文教程为准。
