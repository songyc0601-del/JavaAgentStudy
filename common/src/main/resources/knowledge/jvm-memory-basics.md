---
id: builtin:jvm-memory-basics
title: JVM 内存区域划分与垃圾回收入门
topic: JVM 内存基础
---

# JVM 内存区域划分与垃圾回收入门

## 核心原理

JVM 运行时数据区按是否被线程共享分为两大类：

- **线程私有区域**：
  - **程序计数器（PC Register）**：记录当前线程正在执行的字节码指令地址，是唯一不会抛出 `OutOfMemoryError` 的区域。
  - **Java 虚拟机栈（JVM Stack）**：每次方法调用都会压入一个栈帧，保存局部变量表、操作数栈与返回地址；递归过深会触发 `StackOverflowError`。
- **线程共享区域**：
  - **堆（Heap）**：存放几乎所有对象实例与数组，是垃圾回收（GC）的主战场。现代分代回收器通常按对象存活时间划分为新生代（Eden、Survivor）与老年代，因为绝大多数对象都是朝生夕死的。
  - **方法区 / 元空间（Metaspace）**：存放类元信息、字段与方法描述、运行时常量池；JDK 8 起元数据移至本地内存（Metaspace），不再占用堆空间。
- **垃圾回收判定**：JVM 使用**可达性分析算法**，从 GC Roots（如栈帧局部变量引用的对象、静态变量引用的对象、活跃线程等）出发遍历引用链，无法到达的对象即为可回收对象。

## 典型场景

- 排查接口偶发卡顿：关注 GC 日志中的 Minor GC / Full GC 频率与停顿时间（STW）。
- 定位服务内存持续上涨：导出堆转储（Heap Dump）分析支配树，定位是谁持有大量本应释放的对象引用。
- 调整容器内 Java 服务内存：显式配置 `-Xms`、`-Xmx`（或 `-XX:MaxRAMPercentage`）以及元空间上限 `-XX:MaxMetaspaceSize`。

## 常见误区

1. **把“栈存基本类型、堆存对象”理解成绝对规则**：对象内部的 `int` 等基本类型字段随对象一起存放在堆中；局部变量表里存放的是基本类型的值或指向堆中对象的引用。
2. **以为有 GC 就不会发生内存泄漏**：若长生命周期集合（如静态 `Map`、`ThreadLocal` 未 `remove`）持续持有不再使用的对象引用，对象始终从 GC Roots 可达，GC 无法回收，最终仍会引发 `OutOfMemoryError: Java heap space`。
3. **手动调用 `System.gc()` 强制立刻回收**：`System.gc()` 只是向 JVM 发出建议，且可能触发代价高昂的 Full GC，业务代码不应主动调用。
4. **混淆 `StackOverflowError` 与 `OutOfMemoryError`**：前者通常是无终止条件的递归耗尽线程栈深度，后者通常是堆、元空间或本地内存耗尽。
