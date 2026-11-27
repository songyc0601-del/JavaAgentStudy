package dev.javaagentstudy.stage01.exercise;

import dev.javaagentstudy.common.domain.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Exercise 生成请求。
 *
 * @param topic      要出题的 Java 主题，不能为空白
 * @param difficulty 可选难度，缺省时由服务端采用初级默认值
 * @param focus      可选的关注重点
 */
@Schema(description = "Exercise 生成请求")
public record ExerciseRequest(
        @Schema(description = "要出题的 Java Topic，不能为空白", example = "集合框架",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "topic must not be blank")
        String topic,
        @Schema(description = "可选难度级别，缺省时默认采用 BEGINNER", example = "BEGINNER", nullable = true)
        Difficulty difficulty,
        @Schema(description = "可选的关注重点", example = "HashMap 扩容机制", nullable = true)
        String focus) {
}
