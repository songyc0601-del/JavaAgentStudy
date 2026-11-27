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
        // Spring AI 生成的 JSON Schema 默认把所有属性标为 required，校验 Advisor 会拦下缺失字段并让模型重试，
        // 所以这里不再静默兜底为空列表，缺失时直接失败（List.copyOf 抛 NPE，由服务层映射为 502），避免返回残缺的 Exercise。
        hints = List.copyOf(hints);
        referenceAnswerPoints = List.copyOf(referenceAnswerPoints);
    }
}
