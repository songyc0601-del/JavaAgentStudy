package dev.javaagentstudy.stage01.explain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 概念讲解响应：原样返回 topic，并附上模型生成的讲解文本。
 *
 * @param topic       被讲解的主题
 * @param explanation 模型生成的概念讲解
 */
@Schema(description = "同步概念讲解响应")
public record ExplanationResponse(
        @Schema(description = "被讲解的 Java Topic", example = "集合框架")
        String topic,
        @Schema(description = "模型生成的概念讲解文本")
        String explanation) {
}
