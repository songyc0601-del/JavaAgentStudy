package dev.javaagentstudy.stage01.explain;

/**
 * 概念讲解响应：原样返回 topic，并附上模型生成的讲解文本。
 *
 * @param topic       被讲解的主题
 * @param explanation 模型生成的概念讲解
 */
public record ExplanationResponse(String topic, String explanation) {
}
