package dev.javaagentstudy.common.domain;

import java.util.List;

/**
 * Exercise：围绕特定 Topic 生成的单个练习任务，用于检验 Learner 的理解程度。
 *
 * <p>不可变 record；列表字段在构造时做防御性拷贝，避免外部修改。
 *
 * @param title                 题目标题
 * @param topic                 所属 Topic
 * @param difficulty            难度级别
 * @param description           题干描述
 * @param hints                 解题提示列表
 * @param referenceAnswerPoints 参考答案要点
 */
public record Exercise(String title, Topic topic, Difficulty difficulty, String description,
        List<String> hints, List<String> referenceAnswerPoints) {

    public Exercise {
        // 强类型契约守门：即使模型返回的 JSON 语法合法或只缺内容（空字符串、空数组），
        // 也在这里直接拒绝，由服务层映射为 502（EXERCISE_GENERATION_FAILED），避免返回残缺的 Exercise。
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Exercise title must not be blank");
        }
        if (topic == null) {
            throw new IllegalArgumentException("Exercise topic must not be null");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("Exercise difficulty must not be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Exercise description must not be blank");
        }
        hints = copyNonEmptyItems(hints, "hints");
        referenceAnswerPoints = copyNonEmptyItems(referenceAnswerPoints, "referenceAnswerPoints");
    }

    private static List<String> copyNonEmptyItems(List<String> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Exercise " + fieldName + " must not be empty");
        }
        List<String> copy = List.copyOf(items);
        for (String item : copy) {
            if (item == null || item.isBlank()) {
                throw new IllegalArgumentException("Exercise " + fieldName + " must not contain blank items");
            }
        }
        return copy;
    }
}
