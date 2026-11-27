---
id: builtin:concurrency-basics
title: Java 并发编程基础与线程安全入门
topic: 并发入门
---

# Java 并发编程基础与线程安全入门

## 核心原理

并发编程需要同时关注三个核心性质：**原子性**（操作不可分割）、**可见性**（一个线程的修改能及时被其他线程看到）与**有序性**（防止指令重排破坏跨线程依赖）。

- **`synchronized`**：Java 内置互斥锁，进入同步块时获取监视器锁并刷新共享变量可见性，退出（包括异常退出）时自动释放锁，同时保证原子性、可见性与有序性。
- **`volatile`**：保证变量的**可见性**与禁止特定指令重排，但**不保证复合操作的原子性**；适合状态标志位或一次写入、多处读取的可见性发布。
- **JUC 并发工具**：
  - 原子类（如 `AtomicInteger`、`LongAdder`）基于 CAS 实现无锁原子更新；
  - 并发容器（如 `ConcurrentHashMap`、`CopyOnWriteArrayList`）在安全与吞吐之间做精细权衡；
  - 线程池（`ExecutorService` / `ThreadPoolExecutor`）复用线程、控制并发度；JDK 21 还引入了轻量级虚拟线程（Virtual Threads），适合高并发 I/O 阻塞型任务。

## 典型场景

- 高并发计数器或指标累加：优先使用 `AtomicLong` 或高竞争场景下的 `LongAdder`。
- 多线程共享缓存与映射表：使用 `ConcurrentHashMap`，并借助 `computeIfAbsent` 等原子组合方法避免先查后插竞态。
- 异步执行 I/O 或批量任务：使用有界队列的线程池或 JDK 21 虚拟线程，明确超时与异常处理策略。

## 常见误区

1. **误以为 `volatile int count` 执行 `count++` 是线程安全的**：`count++` 包含“读—改—写”三步，多线程下仍会丢更新，必须使用 `AtomicInteger` 或加锁。
2. **用 `Executors.newFixedThreadPool` 或 `newCachedThreadPool` 直接上生产**：前者默认无界任务队列容易堆积导致 OOM，后者允许创建近乎无限的线程；建议显式构造 `ThreadPoolExecutor` 指定核心数、最大数、有界队列与拒绝策略。
3. **锁粒度过大或锁对象可变**：对整个方法加 `synchronized` 会拖慢吞吐；对可能被重新赋值的字段或字符串字面量加锁会导致锁失效或意外竞争，应锁在 `private final Object lock = new Object()` 上。
4. **忽略 `ConcurrentHashMap` 组合操作的非原子性**：先调用 `containsKey` 再调用 `put` 仍存在竞态窗口，应改用 `putIfAbsent`、`compute` 或 `merge`。
