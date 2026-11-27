package dev.javaagentstudy.stage01.explain;

import jakarta.validation.constraints.NotBlank;

/**
 * 概念讲解请求：Learner 想学的 Topic，以及可选的关注重点。
 *
 * <p>record 是 Java 的不可变数据类，字段一旦创建就不能修改。
 *
 * @param topic 要讲解的 Java 主题，例如“集合框架”。不能为空白；
 *              @NotBlank 同时拒绝 null、空字符串和只有空格的字符串
 * @param focus 可选的关注重点，不需要时可以为 null
 */
public record ExplanationRequest(@NotBlank(message = "topic must not be blank") String topic, String focus) {
}
