package dev.javaagentstudy.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * KnowledgeSource 领域记录的行为测试。
 */
class KnowledgeSourceTest {

    @Test
    void keepsValidFields() {
        Topic topic = new Topic("集合框架");
        KnowledgeSource source = new KnowledgeSource(
                "builtin:collections-hashmap",
                "集合框架与 HashMap 核心原理",
                topic,
                "HashMap 基于数组、链表和红黑树实现键值存储。");

        assertThat(source.id()).isEqualTo("builtin:collections-hashmap");
        assertThat(source.title()).isEqualTo("集合框架与 HashMap 核心原理");
        assertThat(source.topic()).isEqualTo(topic);
        assertThat(source.content()).isEqualTo("HashMap 基于数组、链表和红黑树实现键值存储。");
    }

    @Test
    void rejectsBlankIdTitleContentOrNullTopic() {
        Topic topic = new Topic("集合框架");

        assertThatThrownBy(() -> new KnowledgeSource(" ", "标题", topic, "正文"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KnowledgeSource("id-1", " ", topic, "正文"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KnowledgeSource("id-1", "标题", null, "正文"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KnowledgeSource("id-1", "标题", topic, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
