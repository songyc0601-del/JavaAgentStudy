package dev.javaagentstudy.common.domain;

/**
 * Exercise 的难度级别。
 *
 * <p>用枚举而不是裸字符串，是为了让非法取值在反序列化阶段就被拒绝。
 */
public enum Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}
