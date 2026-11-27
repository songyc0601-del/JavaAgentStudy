# 采用 Maven 多模块按学习阶段隔离代码

本仓库同时承担「初级 Java 开发者循序渐进学 AI Agent」与「最终整合为可运行的 Study Agent」双重目标。我们选择采用 **Maven 多模块按阶段隔离**（`common` + `stage-01` 至 `stage-06` + `study-agent-app`），而非单模块单应用加 Git 分支/包名切分。虽然多模块会在相邻阶段间产生适度重复代码，但它能确保每个阶段的依赖与 Spring Boot 自动装配保持纯净、随时可独立启动和回看对比，避免后期引入数据库、向量库或 MCP Starter 污染早期基础示例。
