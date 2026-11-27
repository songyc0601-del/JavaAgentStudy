---
id: builtin:spring-ioc-aop-pitfalls
title: Spring IoC 与 AOP 核心机制及常见误区
topic: Spring IoC 与 AOP
---

# Spring IoC 与 AOP 核心机制及常见误区

## 核心原理

- **IoC（控制反转）与 DI（依赖注入）**：由 Spring 容器统一负责 Bean 的创建、装配与生命周期管理，应用代码只需声明依赖，无需手工 `new` 协作对象。官方最推荐**构造器注入**：依赖不可变（可用 `final`）、在启动期即可暴露缺失依赖，且脱离容器也能直接编写单元测试。
- **Bean 作用域与生命周期**：默认作用域为 `singleton`（同一 Spring 容器内全局共享唯一实例），因此 Service / Controller / Repository 等组件必须保持无状态，避免在成员变量中保存请求级用户数据。
- **AOP（面向切面编程）与动态代理**：Spring AOP 通过为目标 Bean 生成代理对象（基于接口的 JDK 动态代理或基于子类的 CGLIB 代理），在不侵入业务代码的前提下织入事务（`@Transactional`）、日志、鉴权等横切逻辑。调用必须经过代理对象，切面才会生效。

## 典型场景

- 用构造器注入组装 Controller、Service 与外部客户端（如 Spring AI 的 `ChatClient.Builder`）。
- 用 `@Transactional` 管理数据库多步写入的原子提交与异常回滚。
- 用切面统一记录接口耗时、审计日志或权限校验。

## 常见误区

1. **同类内部自调用导致 `@Transactional` / `@Async` / `@Cacheable` 失效**：在类内部直接通过 `this.otherMethod()` 调用另一个标注了切面注解的方法时，绕过了 Spring 代理对象，切面完全不会触发；应将该方法拆分到独立 Bean 中注入调用。
2. **在单例 Bean 中保存可变请求状态**：将当前请求参数或用户信息存到 `@Service` 字段中，高并发下不同请求会互相覆盖数据；状态应通过方法参数或返回值传递。
3. **`@Transactional` 默认只对运行时异常回滚**：抛出受检异常（Checked Exception）默认不会触发回滚，必要时需显式声明 `rollbackFor = Exception.class`；同时切忌在事务方法内部 `catch` 异常后吞掉不重新抛出。
4. **滥用字段注入（`@Autowired` 写在私有字段上）**：隐藏了类的真实依赖数量，容易滋生循环依赖，也不利于纯 Java 单元测试；应优先使用构造器注入。
