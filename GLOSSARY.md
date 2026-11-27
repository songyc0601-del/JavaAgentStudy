# JavaAgentStudy

围绕「面向初级 Java 开发者的学习助手（Study Agent）」展开的渐进式 AI Agent 实战学习项目。

## Language

### Agent 核心

**Agent**:
由大语言模型驱动、能够基于目标进行推理并借助工具执行动作的智能体。
_Avoid_: javaagent, bot, chatbot

**Study Agent**:
本项目最终构建并持续演进的核心应用，专用于辅助初级 Java 开发者学习与练习 Java 技能的 Agent。
_Avoid_: 学习机器人, 刷题工具, 问答脚本

### 学习领域

**Learner**:
使用 Study Agent 进行 Java 学习的单一开发者。
_Avoid_: User, Student, Account

**Topic**:
Java 学习体系中的一个独立知识主题，例如集合框架、JVM 内存模型或并发基础。
_Avoid_: Subject, Tag, Category

**Explanation**:
Study Agent 针对某个 Topic（可附带 Learner 的关注重点）生成的概念讲解，面向初级 Java 开发者，涵盖核心原理、典型场景与常见误区。
_Avoid_: Answer, Summary, Tutorial

**Knowledge Source**:
供 Study Agent 检索与引用、用于支撑概念讲解与出题的原始学习资料。
_Avoid_: Document, Corpus, File

**Study Session**:
Learner 与 Study Agent 之间一次连续的多轮交互会话，承载当次对话上下文。
_Avoid_: Chat, Conversation, Thread

**Exercise**:
围绕特定 Topic 生成的单个练习任务，用于检验 Learner 的理解程度。
_Avoid_: Problem, Question, Quiz

**Difficulty**:
Exercise 的难度级别，分为初级、中级、高级三档；未指定时默认初级。
_Avoid_: Level, Grade, Hardness

**Attempt**:
Learner 针对某道 Exercise 提交的一次具体作答（文字解释或代码片段）；同一道 Exercise 可有多次 Attempt。
_Avoid_: Answer, Submission, Try

**Review**:
Study Agent 对单次 Attempt 给出的评审反馈，包含正误判断、问题点与改进建议。
_Avoid_: Feedback, Comment, Evaluation

**Mistake Entry**:
按 Topic 归类并跨 Study Session 长期保留的错题与薄弱点记录，支持从待攻克流转为已掌握而不删除历史。
_Avoid_: Error Log, Bug, Wrong Question
