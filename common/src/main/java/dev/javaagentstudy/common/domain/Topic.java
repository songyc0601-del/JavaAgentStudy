package dev.javaagentstudy.common.domain;

/**
 * Topic：Java 学习体系中的一个独立知识主题，例如“集合框架”。
 *
 * <p>这是一个不可变的 record，创建后不能修改。
 * 用专用类型而不是裸字符串，是为了让代码里的“主题”一眼就能看懂，
 * 同时把“主题不能为空”这条规则集中放在一个地方。
 *
 * @param name 主题名称，不能为 null 或空白
 */
public record Topic(String name) {

    // 紧凑构造器：每次创建 Topic 时都会先执行这里的检查。
    public Topic {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Topic name must not be blank");
        }
    }
}
