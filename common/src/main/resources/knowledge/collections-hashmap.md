---
id: builtin:collections-hashmap
title: 集合框架与 HashMap 核心原理
topic: 集合框架
---

# 集合框架与 HashMap 核心原理

## 核心原理

Java 集合框架主要分为单列集合 `Collection`（`List`、`Set`、`Queue`）与双列键值映射 `Map` 两大体系：

- **`ArrayList` vs `LinkedList`**：`ArrayList` 底层是动态扩容数组，按索引随机访问为 $O(1)$，尾插均摊 $O(1)$；`LinkedList` 是双向链表，按索引访问为 $O(n)$，现代 CPU 缓存局部性也明显弱于数组，因此绝大多数列表场景优先选择 `ArrayList`。
- **`HashMap` 存储结构（JDK 8+）**：底层采用「数组 + 链表 + 红黑树」。插入键值时先对 `key.hashCode()` 做扰动计算并定位桶（bucket）下标；若发生哈希冲突则挂入链表；当单个桶的链表长度达到 8 且数组容量达到 64 时转换为红黑树，将极端冲突下的查找复杂度从 $O(n)$ 降至 $O(\log n)$。
- **扩容机制**：默认初始容量 16、负载因子 0.75，元素数量超过 `capacity * loadFactor` 时按 2 倍扩容并重新分散节点；由于容量始终为 2 的幂，定位下标可用 `(n - 1) & hash` 替代取模运算。

## 典型场景

- 需要按顺序存储并频繁下标读取：优先使用 `ArrayList`。
- 需要去重或快速判断元素是否存在：使用 `HashSet`（保持插入顺序用 `LinkedHashSet`）。
- 需要通过键快速检索对象或做内存计数/分组：使用 `HashMap`；若已知大致元素数 `expectedSize`，可用 `HashMap.newHashMap(expectedSize)`（JDK 19+）减少扩容开销。

## 常见误区

1. **只重写 `equals` 不重写 `hashCode`**：两个逻辑相等的对象若 `hashCode` 不同，会被放进 `HashMap` 的不同桶中，导致 `get` 返回 `null` 或 `HashSet` 出现重复元素。
2. **用可变对象作为 `HashMap` 的 Key**：放入 `HashMap` 后再修改影响 `hashCode` 的字段，后续将无法再通过原 Key 定位到该条目，造成“查不到也删不掉”的内存泄漏。
3. **在 `for-each` 遍历时直接调用 `list.remove(...)` 或 `map.remove(...)`**：会触发 `ConcurrentModificationException`；应使用 `Iterator.remove()` 或 `removeIf(...)`。
4. **多线程共享 `HashMap`**：`HashMap` 非线程安全，并发写入可能造成数据覆盖或丢失，并发场景应改用 `ConcurrentHashMap`。
