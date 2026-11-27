package dev.javaagentstudy.stage01.exercise;

import dev.javaagentstudy.common.domain.Difficulty;
import jakarta.validation.constraints.NotBlank;

/**
 * Exercise 生成请求。
 *
 * @param topic      要出题的 Java 主题，不能为空白
 * @param difficulty 可选难度，缺省时由服务端采用初级默认值
 * @param focus      可选的关注重点
 */
public record ExerciseRequest(@NotBlank(message = "topic must not be blank") String topic,
        Difficulty difficulty, String focus) {
}
